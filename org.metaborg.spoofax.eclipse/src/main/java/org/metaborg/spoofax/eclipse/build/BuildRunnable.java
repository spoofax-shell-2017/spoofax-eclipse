package org.metaborg.spoofax.eclipse.build;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.metaborg.core.analysis.AnalysisFileResult;
import org.metaborg.core.analysis.AnalysisResult;
import org.metaborg.core.build.BuildInput;
import org.metaborg.core.build.IBuildOutput;
import org.metaborg.core.build.IBuilder;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.processing.ICancellationToken;
import org.metaborg.core.processing.IProgressReporter;
import org.metaborg.core.syntax.ParseResult;
import org.metaborg.spoofax.eclipse.processing.EclipseProgressReporter;
import org.metaborg.spoofax.eclipse.resource.EclipseProject;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.eclipse.util.MarkerUtils;
import org.metaborg.spoofax.eclipse.util.Nullable;
import org.metaborg.util.Ref;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BuildRunnable<P, A, T> implements IWorkspaceRunnable {
    private static final Logger logger = LoggerFactory.getLogger(BuildRunnable.class);

    private final IEclipseResourceService resourceService;
    private final IBuilder<P, A, T> builder;
    private final BuildInput input;
    private final ICancellationToken cancellationToken;
    private final Ref<IBuildOutput<P, A, T>> outputRef;
    
    private @Nullable IProgressReporter progressReporter;


    public BuildRunnable(IEclipseResourceService resourceService, IBuilder<P, A, T> builder, BuildInput input,
        @Nullable IProgressReporter progressReporter, ICancellationToken cancellationToken,
        Ref<IBuildOutput<P, A, T>> outputRef) {
        this.resourceService = resourceService;
        this.builder = builder;
        this.input = input;
        this.cancellationToken = cancellationToken;
        this.outputRef = outputRef;
        
        this.progressReporter = progressReporter;
    }


    @Override public void run(IProgressMonitor monitor) throws CoreException {
        if(progressReporter == null) {
            progressReporter = new EclipseProgressReporter(monitor);
        }
        
        final IBuildOutput<P, A, T> output = builder.build(input, progressReporter, cancellationToken);
        final IProject eclipseProject = ((EclipseProject) input.project).eclipseProject;
        MarkerUtils.clearAll(eclipseProject);

        for(FileObject resource : output.changedResources()) {
            final IResource eclipseResource = resourceService.unresolve(resource);
            if(eclipseResource == null) {
                logger.error("Cannot clear markers for {}", resource);
                continue;
            }
            MarkerUtils.clearAll(eclipseResource);
        }

        for(ParseResult<P> result : output.parseResults()) {
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

        for(AnalysisResult<P, A> result : output.analysisResults()) {
            for(AnalysisFileResult<P, A> fileResult : result.fileResults) {
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

        outputRef.set(output);
    }
}
