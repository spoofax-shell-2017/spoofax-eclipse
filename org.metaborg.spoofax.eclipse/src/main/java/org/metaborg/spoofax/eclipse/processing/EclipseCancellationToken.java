package org.metaborg.spoofax.eclipse.processing;

import org.eclipse.core.runtime.IProgressMonitor;
import org.metaborg.core.processing.ICancellationToken;

public class EclipseCancellationToken implements ICancellationToken {
    private final IProgressMonitor progressMonitor;


    public EclipseCancellationToken(IProgressMonitor progressMonitor) {
        this.progressMonitor = progressMonitor;
    }


    public void cancel() {
        progressMonitor.setCanceled(true);
    }

    @Override public boolean cancelled() {
        return progressMonitor.isCanceled();
    }

    @Override public void throwIfCancelled() throws InterruptedException {
        if(progressMonitor.isCanceled()) {
            throw new InterruptedException();
        }
    }
}
