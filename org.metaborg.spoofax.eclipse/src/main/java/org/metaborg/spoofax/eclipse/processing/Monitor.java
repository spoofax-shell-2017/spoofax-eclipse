package org.metaborg.spoofax.eclipse.processing;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.metaborg.core.processing.ICancel;
import org.metaborg.core.processing.IProgress;

public class Monitor implements IProgress, ICancel {
    public final SubMonitor monitor;


    public Monitor(IProgressMonitor monitor) {
        this.monitor = SubMonitor.convert(monitor);
    }

    public Monitor(SubMonitor monitor) {
        this.monitor = monitor;
    }


    @Override public void work(int ticks) {
        monitor.worked(ticks);
    }

    @Override public void setWorkRemaining(int ticks) {
        monitor.setWorkRemaining(ticks);
    }

    @Override public IProgress subProgress(int ticks) {
        return new Monitor(monitor.newChild(ticks));
    }


    @Override public void cancel() {
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
