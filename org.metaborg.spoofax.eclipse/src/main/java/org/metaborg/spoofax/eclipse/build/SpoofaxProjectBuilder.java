package org.metaborg.spoofax.eclipse.build;

import java.util.Collection;
import java.util.Map;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.build.BuildInput;
import org.metaborg.core.build.BuildInputBuilder;
import org.metaborg.core.build.BuildState;
import org.metaborg.core.build.CleanInput;
import org.metaborg.core.build.CleanInputBuilder;
import org.metaborg.core.build.IBuildOutput;
import org.metaborg.core.build.dependency.IDependencyService;
import org.metaborg.core.build.dependency.MissingDependencies;
import org.metaborg.core.build.paths.ILanguagePathService;
import org.metaborg.core.processing.ITask;
import org.metaborg.core.project.IProject;
import org.metaborg.core.project.IProjectService;
import org.metaborg.core.resource.ResourceChange;
import org.metaborg.core.resource.ResourceChangeKind;
import org.metaborg.core.resource.ResourceUtils;
import org.metaborg.core.transform.CompileGoal;
import org.metaborg.spoofax.core.processing.ISpoofaxProcessorRunner;
import org.metaborg.spoofax.core.resource.SpoofaxIgnoresSelector;
import org.metaborg.spoofax.eclipse.SpoofaxPlugin;
import org.metaborg.spoofax.eclipse.processing.EclipseCancellationToken;
import org.metaborg.spoofax.eclipse.processing.EclipseProgressReporter;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.eclipse.util.Nullable;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.terms.IStrategoTerm;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Injector;

public class SpoofaxProjectBuilder extends IncrementalProjectBuilder {
    private static final ILogger logger = LoggerUtils.logger(SpoofaxProjectBuilder.class);

    public static final String id = SpoofaxPlugin.id + ".builder";

    private final IEclipseResourceService resourceService;
    private final ILanguagePathService languagePathService;
    private final IProjectService projectService;
    private final IDependencyService dependencyService;
    private final ISpoofaxProcessorRunner processorRunner;

    private final Map<org.eclipse.core.resources.IProject, BuildState> states = Maps.newHashMap();


    public SpoofaxProjectBuilder() {
        final Injector injector = SpoofaxPlugin.injector();
        this.resourceService = injector.getInstance(IEclipseResourceService.class);
        this.languagePathService = injector.getInstance(ILanguagePathService.class);
        this.projectService = injector.getInstance(IProjectService.class);
        this.dependencyService = injector.getInstance(IDependencyService.class);
        this.processorRunner = injector.getInstance(ISpoofaxProcessorRunner.class);
    }


    @Override protected org.eclipse.core.resources.IProject[] build(int kind, Map<String, String> args,
        IProgressMonitor monitor) throws CoreException {
        final org.eclipse.core.resources.IProject eclipseProject = getProject();
        final FileObject location = resourceService.resolve(eclipseProject);
        final IProject project = projectService.get(location);
        if(project == null) {
            logger.error("Cannot build project, cannot retrieve Metaborg project for {}", eclipseProject);
            cancel(monitor);
            return null;
        }

        final MissingDependencies missing = dependencyService.checkDependencies(project);
        if(!missing.empty()) {
            logger.error("Cannot build project {}, some dependencies are missing.\n{}", project, missing.toString());
            cancel(monitor);
            return null;
        }

        try {
            final ITask<IBuildOutput<IStrategoTerm, IStrategoTerm, IStrategoTerm>> task;
            if(kind == FULL_BUILD) {
                task = fullBuild(project, monitor);
            } else {
                final IResourceDelta delta = getDelta(eclipseProject);
                if(delta == null) {
                    task = fullBuild(project, monitor);
                } else {
                    task = incrBuild(project, states.get(eclipseProject), delta, monitor);
                }
            }

            task.schedule().block();
            if(task.cancelled()) {
                cancel(monitor);
            } else {
                final IBuildOutput<?, ?, ?> output = task.result();
                if(output != null) {
                    states.put(eclipseProject, output.state());
                }
            }
        } catch(InterruptedException e) {
            cancel(monitor);
        } catch(MetaborgException | FileSystemException e) {
            cancel(monitor);
            logger.error("Cannot build project {}; build failed unexpectedly", e, project);
        }

        // Return value is used to declare dependencies on other projects, but right now this is not possible in
        // Spoofax, so always return null.
        return null;
    }

    private @Nullable ITask<IBuildOutput<IStrategoTerm, IStrategoTerm, IStrategoTerm>> fullBuild(
        org.metaborg.core.project.IProject project, IProgressMonitor monitor) throws InterruptedException,
        FileSystemException, MetaborgException {
        final Iterable<FileObject> resources = ResourceUtils.find(project.location());
        final Iterable<ResourceChange> creations = ResourceUtils.toChanges(resources, ResourceChangeKind.Create);
        processorRunner.updateDialects(project.location(), creations).schedule().block();

        final BuildInputBuilder inputBuilder = new BuildInputBuilder(project);
        // @formatter:off
        final BuildInput input = inputBuilder
            .withDefaultIncludePaths(true)
            .withSourcesFromDefaultSourceLocations(true)
            .withSelector(new SpoofaxIgnoresSelector())
            .addTransformGoal(new CompileGoal())
            .build(dependencyService, languagePathService)
            ;
        // @formatter:on

        return processorRunner
            .build(input, new EclipseProgressReporter(monitor), new EclipseCancellationToken(monitor));
    }

    private ITask<IBuildOutput<IStrategoTerm, IStrategoTerm, IStrategoTerm>> incrBuild(IProject project,
        @Nullable BuildState state, IResourceDelta delta, IProgressMonitor monitor) throws CoreException,
        InterruptedException, MetaborgException {
        final Collection<ResourceChange> changes = Lists.newLinkedList();
        delta.accept(new IResourceDeltaVisitor() {
            @Override public boolean visit(IResourceDelta innerDelta) throws CoreException {
                final ResourceChange change = resourceService.resolve(innerDelta);
                if(change != null) {
                    changes.add(change);
                }
                return true;
            }
        });

        processorRunner.updateDialects(project.location(), changes).schedule().block();

        final BuildInputBuilder inputBuilder = new BuildInputBuilder(project);
        // @formatter:off
        final BuildInput input = inputBuilder
            .withState(state)
            .withDefaultIncludePaths(true)
            .withSourceChanges(changes)
            .withSelector(new SpoofaxIgnoresSelector())
            .addTransformGoal(new CompileGoal())
            .build(dependencyService, languagePathService)
            ;
        // @formatter:on

        return processorRunner
            .build(input, new EclipseProgressReporter(monitor), new EclipseCancellationToken(monitor));
    }

    private void cancel(IProgressMonitor monitor) {
        rememberLastBuiltState();
        monitor.setCanceled(true);
    }


    @Override protected void clean(IProgressMonitor monitor) throws CoreException {
        final org.eclipse.core.resources.IProject eclipseProject = getProject();
        final FileObject location = resourceService.resolve(eclipseProject);
        final IProject project = projectService.get(location);
        if(project == null) {
            logger.error("Cannot clean project, cannot retrieve Metaborg project for {}", eclipseProject);
            monitor.setCanceled(true);
            return;
        }

        try {
            clean(project, monitor).schedule().block();
        } catch(InterruptedException e) {
            monitor.setCanceled(true);
        } catch(MetaborgException e) {
            monitor.setCanceled(true);
            logger.error("Cannot clean project {}; cleaning failed unexpectedly", e, project);
        } finally {
            forgetLastBuiltState();
            states.remove(project);
        }
    }

    private ITask<?> clean(IProject project, IProgressMonitor monitor) throws MetaborgException {
        final CleanInputBuilder inputBuilder = new CleanInputBuilder(project);
        // @formatter:off
        final CleanInput input = inputBuilder
            .withSelector(new SpoofaxIgnoresSelector())
            .build(dependencyService)
            ;
        // @formatter:on

        return processorRunner
            .clean(input, new EclipseProgressReporter(monitor), new EclipseCancellationToken(monitor));
    }
}
