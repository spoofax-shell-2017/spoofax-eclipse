package org.metaborg.spoofax.eclipse.processing;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;

public class Monitor implements IProgress, ICancel {
    private final SubMonitor monitor;


    public Monitor(IProgressMonitor monitor) {
        this.monitor = SubMonitor.convert(monitor);
    }

    public Monitor(SubMonitor monitor) {
        this.monitor = monitor;
    }


    @Override public void work(int ticks) {
        monitor.worked(ticks);
    }

    @Override public void setDescription(String description) {
        monitor.subTask(description);
    }

    @Override public void setWorkRemaining(int ticks) {
        monitor.setWorkRemaining(ticks);
    }

    @Override public Monitor subProgress(int ticks) {
        return new Monitor(monitor.split(ticks, SubMonitor.SUPPRESS_SETTASKNAME | SubMonitor.SUPPRESS_BEGINTASK));
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


    public IProgressMonitor eclipseMonitor() {
        return monitor;
    }
}
