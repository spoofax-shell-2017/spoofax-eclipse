package org.metaborg.spoofax.eclipse.processing;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.metaborg.core.processing.IProgress;

/**
 * Progress reporter implementation from {@link IProgressMonitor}s in Eclipse.
 */
public class Progress implements IProgress {
    private final SubMonitor monitor;


    public Progress(IProgressMonitor monitor) {
        this.monitor = SubMonitor.convert(monitor);
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

    @Override public Progress subProgress(int ticks) {
        return new Progress(monitor.split(ticks, SubMonitor.SUPPRESS_SETTASKNAME | SubMonitor.SUPPRESS_BEGINTASK));
    }


    public IProgressMonitor eclipseMonitor() {
        return monitor;
    }
}
