package org.metaborg.spoofax.eclipse.processing;

import org.eclipse.core.runtime.IProgressMonitor;
import org.metaborg.core.processing.ICancellationToken;

public class CancellationToken implements ICancellationToken {
    public final IProgressMonitor monitor;


    public CancellationToken(IProgressMonitor monitor) {
        this.monitor = monitor;
    }


    public void cancel() {
        monitor.setCanceled(true);
    }

    @Override public boolean cancelled() {
        return monitor.isCanceled();
    }

    @Override public void throwIfCancelled() throws InterruptedException {
        if(monitor.isCanceled()) {
            throw new InterruptedException();
        }
    }
}
