package org.metaborg.spoofax.eclipse.processing;

import org.eclipse.core.runtime.IProgressMonitor;
import org.metaborg.core.processing.ICancel;

public class Cancel implements ICancel {
    private final IProgressMonitor monitor;


    public Cancel(IProgressMonitor monitor) {
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


    public IProgressMonitor eclipseMonitor() {
        return monitor;
    }
}
