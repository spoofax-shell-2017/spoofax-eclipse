package org.metaborg.spoofax.eclipse.build;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.metaborg.core.analysis.IAnalyzeUnit;
import org.metaborg.core.analysis.IAnalyzeUnitUpdate;
import org.metaborg.core.build.BuildInput;
import org.metaborg.core.build.IBuildOutput;
import org.metaborg.core.build.IBuilder;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.processing.ICancellationToken;
import org.metaborg.core.processing.IProgressReporter;
import org.metaborg.core.syntax.IParseUnit;
import org.metaborg.core.transform.ITransformUnit;
import org.metaborg.spoofax.eclipse.processing.ProgressReporter;
import org.metaborg.spoofax.eclipse.project.EclipseProject;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.eclipse.util.MarkerUtils;
import org.metaborg.spoofax.eclipse.util.Nullable;
import org.metaborg.util.Ref;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

public class BuildRunnable<P extends IParseUnit, A extends IAnalyzeUnit, AU extends IAnalyzeUnitUpdate, T extends ITransformUnit<?>>
    implements IWorkspaceRunnable {
    private static final ILogger logger = LoggerUtils.logger(BuildRunnable.class);

    private final IEclipseResourceService resourceService;
    private final IBuilder<P, A, AU, T> builder;
    private final BuildInput input;
    private final ICancellationToken cancellationToken;
    private final Ref<IBuildOutput<P, A, AU, T>> outputRef;

    private @Nullable IProgressReporter progressReporter;


    public BuildRunnable(IEclipseResourceService resourceService, IBuilder<P, A, AU, T> builder, BuildInput input,
        @Nullable IProgressReporter progressReporter, ICancellationToken cancellationToken,
        Ref<IBuildOutput<P, A, AU, T>> outputRef) {
        this.resourceService = resourceService;
        this.builder = builder;
        this.input = input;
        this.cancellationToken = cancellationToken;
        this.outputRef = outputRef;

        this.progressReporter = progressReporter;
    }


    @Override public void run(IProgressMonitor monitor) throws CoreException {
        if(progressReporter == null) {
            progressReporter = new ProgressReporter(monitor);
        }

        final IBuildOutput<P, A, AU, T> output;
        try {
            output = builder.build(input, progressReporter, cancellationToken);
        } catch(InterruptedException e) {
            return;
        }

        final IProject eclipseProject = ((EclipseProject) input.project).eclipseProject;
        MarkerUtils.clearAll(eclipseProject);

        for(FileObject resource : output.changedResources()) {
            if(output.includedResources().contains(resource.getName())) {
                // Don't clear markers for included resources.
                continue;
            }

            final IResource eclipseResource = resourceService.unresolve(resource);
            if(eclipseResource == null) {
                logger.debug("Cannot clear markers for {}, resource is not in the Eclipse workspace", resource);
                continue;
            }
            MarkerUtils.clearAll(eclipseResource);
        }

        for(P result : output.parseResults()) {
            if(output.includedResources().contains(result.source().getName())) {
                // Don't create markers for included resources.
                continue;
            }

            for(IMessage message : result.messages()) {
                final FileObject resource = message.source();
                if(resource == null) {
                    continue;
                }
                final IResource eclipseResource = resourceService.unresolve(resource);
                if(eclipseResource == null) {
                    logger.debug("Cannot create marker for {}, resource is not in the Eclipse workspace", resource);
                    continue;
                }
                MarkerUtils.createMarker(eclipseResource, message);
            }
        }

        for(A result : output.analysisResults()) {
            if(output.includedResources().contains(result.source().getName())) {
                // Don't create markers for included resources.
                continue;
            }

            for(IMessage message : result.messages()) {
                final FileObject resource = message.source();
                if(output.removedResources().contains(resource.getName())) {
                    // Analysis results contain removed resources, don't create markers for removed
                    // resources.
                    continue;
                }
                final IResource eclipseResource = resourceService.unresolve(resource);
                if(eclipseResource == null) {
                    logger.debug("Cannot create marker for {}, resource is not in the Eclipse workspace", resource);
                    continue;
                }
                MarkerUtils.createMarker(eclipseResource, message);
            }
        }

        for(AU update : output.analysisUpdates()) {
            final FileObject resource = update.source();
            if(output.includedResources().contains(resource.getName())) {
                // Don't create markers for included resources.
                continue;
            }
            final IResource eclipseResource = resourceService.unresolve(resource);
            if(eclipseResource == null) {
                logger.debug("Cannot clear or create markers for {}, resource is not in the Eclipse workspace",
                    resource);
                continue;
            }
            MarkerUtils.clearAnalysis(eclipseResource);
            for(IMessage message : update.messages()) {
                MarkerUtils.createMarker(eclipseResource, message);
            }
        }

        for(IMessage message : output.extraMessages()) {
            final FileObject resource = message.source();
            if(output.includedResources().contains(resource.getName())) {
                // Don't create markers for included resources.
                continue;
            }

            final IResource eclipseResource = resourceService.unresolve(resource);
            if(eclipseResource == null) {
                logger.debug("Cannot create marker for {}, resource is not in the Eclipse workspace", resource);
                continue;
            }
            MarkerUtils.createMarker(eclipseResource, message);
        }

        outputRef.set(output);
    }
}
