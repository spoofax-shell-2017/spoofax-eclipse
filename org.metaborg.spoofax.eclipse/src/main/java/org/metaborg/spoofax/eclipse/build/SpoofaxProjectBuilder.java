package org.metaborg.spoofax.eclipse.build;

import java.util.Collection;
import java.util.Map;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.metaborg.spoofax.core.analysis.AnalysisFileResult;
import org.metaborg.spoofax.core.analysis.AnalysisResult;
import org.metaborg.spoofax.core.build.BuildInput;
import org.metaborg.spoofax.core.build.BuildInputBuilder;
import org.metaborg.spoofax.core.build.IBuildOutput;
import org.metaborg.spoofax.core.build.ISpoofaxBuilder;
import org.metaborg.spoofax.core.build.dependency.IDependencyService;
import org.metaborg.spoofax.core.build.paths.ILanguagePathService;
import org.metaborg.spoofax.core.messages.IMessage;
import org.metaborg.spoofax.core.project.IProjectService;
import org.metaborg.spoofax.core.resource.IResourceChange;
import org.metaborg.spoofax.core.resource.SpoofaxIgnoredDirectories;
import org.metaborg.spoofax.core.syntax.ParseResult;
import org.metaborg.spoofax.core.transform.CompileGoal;
import org.metaborg.spoofax.eclipse.SpoofaxPlugin;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.eclipse.util.MarkerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spoofax.interpreter.terms.IStrategoTerm;

import com.google.common.collect.Lists;
import com.google.inject.Injector;

public class SpoofaxProjectBuilder extends IncrementalProjectBuilder {
    public static final String id = SpoofaxPlugin.id + ".builder";

    private static final Logger logger = LoggerFactory.getLogger(SpoofaxProjectBuilder.class);

    private final IEclipseResourceService resourceService;
    private final ILanguagePathService languagePathService;
    private final IProjectService projectService;
    private final IDependencyService dependencyService;
    private final ISpoofaxBuilder builder;


    public SpoofaxProjectBuilder() {
        final Injector injector = SpoofaxPlugin.injector();
        this.resourceService = injector.getInstance(IEclipseResourceService.class);
        this.languagePathService = injector.getInstance(ILanguagePathService.class);
        this.projectService = injector.getInstance(IProjectService.class);
        this.dependencyService = injector.getInstance(IDependencyService.class);
        this.builder = injector.getInstance(ISpoofaxBuilder.class);
    }


    @Override protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor)
        throws CoreException {
        final IProject project = getProject();
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

        // Return value is used to declare dependencies on other projects, but right now this is
        // not possible in Spoofax, so always return null.
        return null;
    }

    @Override protected void clean(IProgressMonitor monitor) throws CoreException {
        clean(getProject(), monitor);
    }


    private void clean(final IProject project, IProgressMonitor monitor) {
        logger.debug("Cleaning project " + project);
        try {
            final IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
                @Override public void run(IProgressMonitor workspaceMonitor) throws CoreException {
                    MarkerUtils.clearAllRec(project);
                    final FileObject location = resourceService.resolve(project);
                    builder.clean(location, SpoofaxIgnoredDirectories.excludeFileSelector());
                }
            };
            final IWorkspace workspace = ResourcesPlugin.getWorkspace();
            workspace.run(runnable, project, IWorkspace.AVOID_UPDATE, monitor);
        } catch(CoreException e) {
            final String message = String.format("Cannot clean project %s", project);
            logger.error(message, e);
        }
    }

    private void fullBuild(IProject eclipseProject, IProgressMonitor monitor) {
        try {
            final FileObject location = resourceService.resolve(eclipseProject);
            final org.metaborg.spoofax.core.project.IProject project = projectService.get(location);

            final BuildInputBuilder inputBuilder = new BuildInputBuilder(project);
            // @formatter:off
            final BuildInput input = inputBuilder
                .withDefaultIncludeLocations(true)
                .withResourcesFromDefaultSourceLocations(true)
                .withSelector(SpoofaxIgnoredDirectories.includeFileSelector())
                .addGoal(new CompileGoal())
                .build(dependencyService, languagePathService)
                ;
            // @formatter:on

            build(eclipseProject, input, monitor);
        } catch(CoreException e) {
            final String message = String.format("Failed to fully build project %s", eclipseProject);
            logger.error(message, e);
        }
    }

    private void incrBuild(IProject eclipseProject, IResourceDelta delta, IProgressMonitor monitor) {
        try {
            final FileObject location = resourceService.resolve(eclipseProject);
            final org.metaborg.spoofax.core.project.IProject project = projectService.get(location);

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
                .addGoal(new CompileGoal())
                .build(dependencyService, languagePathService)
                ;
            // @formatter:on

            build(eclipseProject, input, monitor);
        } catch(CoreException e) {
            final String message = String.format("Failed to incrementally build project %s", eclipseProject);
            logger.error(message, e);
        }
    }

    private void build(final IProject project, final BuildInput input, IProgressMonitor monitor) throws CoreException {
        final IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
            @Override public void run(IProgressMonitor workspaceMonitor) throws CoreException {
                final IBuildOutput<IStrategoTerm, IStrategoTerm, IStrategoTerm> output = builder.build(input);

                MarkerUtils.clearAll(project);
                for(FileObject resource : output.changedResources()) {
                    final IResource eclipseResource = resourceService.unresolve(resource);
                    if(eclipseResource == null) {
                        logger.error("Cannot clear markers for {}", resource);
                        continue;
                    }
                    MarkerUtils.clearAll(eclipseResource);
                }

                for(ParseResult<IStrategoTerm> result : output.parseResults()) {
                    for(IMessage message : result.messages) {
                        final FileObject resource = message.source();
                        final IResource eclipseResource = resourceService.unresolve(resource);
                        if(eclipseResource == null) {
                            logger.error("Cannot create marker for {}", resource);
                            continue;
                        }
                        MarkerUtils.createMarker(eclipseResource, message);
                    }
                }

                for(AnalysisResult<IStrategoTerm, IStrategoTerm> result : output.analysisResults()) {
                    for(AnalysisFileResult<IStrategoTerm, IStrategoTerm> fileResult : result.fileResults) {
                        for(IMessage message : fileResult.messages) {
                            final FileObject resource = message.source();
                            if(output.removedResources().contains(resource.getName())) {
                                // Analysis results contain removed resources, don't create markers for removed
                                // resources.
                                continue;
                            }
                            final IResource eclipseResource = resourceService.unresolve(resource);
                            if(eclipseResource == null) {
                                logger.error("Cannot create marker for {}", resource);
                                continue;
                            }
                            MarkerUtils.createMarker(eclipseResource, message);
                        }
                    }
                }

                for(IMessage message : output.extraMessages()) {
                    final FileObject resource = message.source();
                    final IResource eclipseResource = resourceService.unresolve(resource);
                    if(eclipseResource == null) {
                        logger.error("Cannot create marker for {}", resource);
                        continue;
                    }
                    MarkerUtils.createMarker(eclipseResource, message);
                }
            }
        };
        final IWorkspace workspace = ResourcesPlugin.getWorkspace();
        workspace.run(runnable, project, IWorkspace.AVOID_UPDATE, monitor);
    }
}
