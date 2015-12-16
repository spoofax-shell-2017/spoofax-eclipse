package org.metaborg.spoofax.eclipse.build;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.metaborg.core.language.dialect.IDialectProcessor;
import org.metaborg.core.processing.ICancellationToken;
import org.metaborg.core.processing.IProgressReporter;
import org.metaborg.core.resource.ResourceChange;
import org.metaborg.spoofax.eclipse.processing.EclipseProgressReporter;
import org.metaborg.spoofax.eclipse.util.Nullable;

public class ProcessDialectsRunnable implements IWorkspaceRunnable {
    private final IDialectProcessor dialectProcessor;
    private final FileObject location;
    private final Iterable<ResourceChange> changes;
    @SuppressWarnings("unused") private final ICancellationToken cancellationToken;

    private IProgressReporter progressReporter;


    public ProcessDialectsRunnable(IDialectProcessor dialectProcessor, FileObject location,
        Iterable<ResourceChange> changes, @Nullable IProgressReporter progressReporter,
        ICancellationToken cancellationToken) {
        this.dialectProcessor = dialectProcessor;
        this.location = location;
        this.changes = changes;
        this.cancellationToken = cancellationToken;

        this.progressReporter = progressReporter;
    }

    @Override public void run(IProgressMonitor monitor) throws CoreException {
        if(progressReporter == null) {
            this.progressReporter = new EclipseProgressReporter(monitor);
        }

        // GTODO: do something with cancellation token.
        dialectProcessor.update(location, changes);
    }
}
