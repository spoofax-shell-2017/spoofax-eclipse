package org.metaborg.spoofax.eclipse.build;

import java.util.Collection;
import java.util.Map;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.metaborg.core.build.BuildInput;
import org.metaborg.core.build.BuildInputBuilder;
import org.metaborg.core.build.CleanInput;
import org.metaborg.core.build.dependency.IDependencyService;
import org.metaborg.core.build.paths.ILanguagePathService;
import org.metaborg.core.project.IProjectService;
import org.metaborg.core.resource.IResourceChange;
import org.metaborg.core.transform.CompileGoal;
import org.metaborg.spoofax.core.processing.ISpoofaxProcessorRunner;
import org.metaborg.spoofax.core.resource.SpoofaxIgnoredDirectories;
import org.metaborg.spoofax.eclipse.SpoofaxPlugin;
import org.metaborg.spoofax.eclipse.processing.EclipseProgressReporter;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;

import com.google.common.collect.Lists;
import com.google.inject.Injector;

public class SpoofaxProjectBuilder extends IncrementalProjectBuilder {
    public static final String id = SpoofaxPlugin.id + ".builder";

    private final IEclipseResourceService resourceService;
    private final ILanguagePathService languagePathService;
    private final IProjectService projectService;
    private final IDependencyService dependencyService;
    private final ISpoofaxProcessorRunner processorRunner;


    public SpoofaxProjectBuilder() {
        final Injector injector = SpoofaxPlugin.injector();
        this.resourceService = injector.getInstance(IEclipseResourceService.class);
        this.languagePathService = injector.getInstance(ILanguagePathService.class);
        this.projectService = injector.getInstance(IProjectService.class);
        this.dependencyService = injector.getInstance(IDependencyService.class);
        this.processorRunner = injector.getInstance(ISpoofaxProcessorRunner.class);
    }


    @Override protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor)
        throws CoreException {
        final IProject project = getProject();

        try {
            if(kind == FULL_BUILD) {
                fullBuild(project, monitor);
            } else {
                final IResourceDelta delta = getDelta(project);
                if(delta == null) {
                    fullBuild(project, monitor);
                } else {
                    incrBuild(project, delta, monitor);
                }
            }
        } catch(InterruptedException e) {
            // Interrupted, build state is invalid, redo entire build next time.
            forgetLastBuiltState();
        }

        // Return value is used to declare dependencies on other projects, but right now this is
        // not possible in Spoofax, so always return null.
        return null;
    }

    @Override protected void clean(IProgressMonitor monitor) throws CoreException {
        try {
            clean(getProject(), monitor);
        } catch(InterruptedException e) {
            // Ignore
        } finally {
            forgetLastBuiltState();
        }
    }


    private void fullBuild(IProject eclipseProject, IProgressMonitor monitor) throws InterruptedException {
        final FileObject location = resourceService.resolve(eclipseProject);
        final org.metaborg.core.project.IProject project = projectService.get(location);

        final BuildInputBuilder inputBuilder = new BuildInputBuilder(project);
        // @formatter:off
        final BuildInput input = inputBuilder
            .withDefaultIncludeLocations(true)
            .withResourcesFromDefaultSourceLocations(true)
            .withSelector(SpoofaxIgnoredDirectories.includeFileSelector())
            .addTransformGoal(new CompileGoal())
            .build(dependencyService, languagePathService)
            ;
        // @formatter:on

        processorRunner.build(input, new EclipseProgressReporter(monitor)).schedule();
    }

    private void incrBuild(IProject eclipseProject, IResourceDelta delta, IProgressMonitor monitor)
        throws CoreException, InterruptedException {
        final FileObject location = resourceService.resolve(eclipseProject);
        final org.metaborg.core.project.IProject project = projectService.get(location);

        final Collection<IResourceChange> changes = Lists.newLinkedList();
        delta.accept(new IResourceDeltaVisitor() {
            @Override public boolean visit(IResourceDelta innerDelta) throws CoreException {
                final IResourceChange change = resourceService.resolve(innerDelta);
                if(change != null) {
                    changes.add(change);
                }
                return true;
            }
        });

        final BuildInputBuilder inputBuilder = new BuildInputBuilder(project);
        // @formatter:off
        final BuildInput input = inputBuilder
            .withDefaultIncludeLocations(true)
            .withResourceChanges(changes)
            .addTransformGoal(new CompileGoal())
            .build(dependencyService, languagePathService)
            ;
        // @formatter:on

        processorRunner.build(input, new EclipseProgressReporter(monitor)).schedule();
    }


    private void clean(final IProject eclipseProject, IProgressMonitor monitor) throws InterruptedException {
        final FileObject location = resourceService.resolve(eclipseProject);
        final org.metaborg.core.project.IProject project = projectService.get(location);
        final CleanInput input = new CleanInput(project, SpoofaxIgnoredDirectories.excludeFileSelector());

        processorRunner.clean(input, new EclipseProgressReporter(monitor)).schedule();
    }
}
