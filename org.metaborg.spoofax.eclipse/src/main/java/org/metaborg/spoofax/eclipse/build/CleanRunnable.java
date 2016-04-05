package org.metaborg.spoofax.eclipse.build;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.metaborg.core.analysis.IAnalyzeUnit;
import org.metaborg.core.analysis.IAnalyzeUnitUpdate;
import org.metaborg.core.build.CleanInput;
import org.metaborg.core.build.IBuilder;
import org.metaborg.core.processing.ICancellationToken;
import org.metaborg.core.processing.IProgressReporter;
import org.metaborg.core.syntax.IParseUnit;
import org.metaborg.core.transform.ITransformUnit;
import org.metaborg.spoofax.eclipse.processing.ProgressReporter;
import org.metaborg.spoofax.eclipse.project.EclipseProject;
import org.metaborg.spoofax.eclipse.util.MarkerUtils;
import org.metaborg.spoofax.eclipse.util.Nullable;

public class CleanRunnable<P extends IParseUnit, A extends IAnalyzeUnit, AU extends IAnalyzeUnitUpdate, T extends ITransformUnit<?>>
    implements IWorkspaceRunnable {
    private final IBuilder<P, A, AU, T> builder;
    private final CleanInput input;
    private final ICancellationToken cancellationToken;

    private IProgressReporter progressReporter;


    public CleanRunnable(IBuilder<P, A, AU, T> builder, CleanInput input, @Nullable IProgressReporter progressReporter,
        ICancellationToken cancellationToken) {
        this.builder = builder;
        this.input = input;
        this.cancellationToken = cancellationToken;

        this.progressReporter = progressReporter;
    }

    @Override public void run(IProgressMonitor monitor) throws CoreException {
        if(progressReporter == null) {
            this.progressReporter = new ProgressReporter(monitor);
        }

        final IProject eclipseProject = ((EclipseProject) input.project).eclipseProject;
        MarkerUtils.clearAllRec(eclipseProject);

        try {
            builder.clean(input, progressReporter, cancellationToken);
        } catch(InterruptedException e) {
        }
    }
}
