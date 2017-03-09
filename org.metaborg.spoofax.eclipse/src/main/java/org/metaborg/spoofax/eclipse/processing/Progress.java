package org.metaborg.spoofax.eclipse.processing;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.metaborg.util.task.IProgress;

/**
 * Progress reporter implementation from {@link IProgressMonitor}s in Eclipse.
 */
public class Progress implements IProgress {
    public final SubMonitor monitor;


    public Progress(IProgressMonitor monitor) {
        this.monitor = SubMonitor.convert(monitor);
    }


    @Override public void work(int ticks) {
        monitor.worked(ticks);
    }

    @Override public void setWorkRemaining(int ticks) {
        monitor.setWorkRemaining(ticks);
    }

    @Override public IProgress subProgress(int ticks) {
        return new Progress(monitor.newChild(ticks));
    }
}
