package org.metaborg.spoofax.eclipse.processing;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.metaborg.core.processing.IProgressReporter;

/**
 * Progress reporter implementation from {@link IProgressMonitor}s in Eclipse.
 */
public class EclipseProgressReporter implements IProgressReporter {
    private final SubMonitor monitor;


    public EclipseProgressReporter(IProgressMonitor monitor) {
        this.monitor = SubMonitor.convert(monitor);
    }


    @Override public void work(int ticks) {
        monitor.worked(ticks);
    }

    @Override public void setWorkRemaining(int ticks) {
        monitor.setWorkRemaining(ticks);
    }

    @Override public IProgressReporter subProgress(int ticks) {
        return new EclipseProgressReporter(monitor.newChild(ticks));
    }
}
