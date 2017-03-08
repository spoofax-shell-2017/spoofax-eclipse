package org.metaborg.spoofax.eclipse.processing;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.metaborg.core.processing.ICancel;
import org.metaborg.core.processing.ITask;
import org.metaborg.spoofax.eclipse.util.ValueStatus;

/**
 * Task implementation for Eclipse jobs.
 */
public class JobTask<T> implements ITask<T> {
    private final Job job;
    private final ICancel cancel;


    public JobTask(Job job, ICancel cancel) {
        this.job = job;
        this.cancel = cancel;
    }


    @Override public ITask<T> schedule() {
        job.schedule();
        return this;
    }

    @Override public void cancel() {
        job.cancel();
        cancel.cancel();
    }

    @Override public void cancel(int forceTimeout) {
        job.cancel();
        cancel.cancel();
        // TODO: kill after timeout
    }

    @Override public boolean cancelled() {
        return cancel.cancelled();
    }

    @Override public boolean completed() {
        return job.getResult() != null;
    }

    @SuppressWarnings("unchecked") @Override public T result() {
        final IStatus status = job.getResult();
        if(status != null && status instanceof ValueStatus) {
            final ValueStatus valueStatus = (ValueStatus) status;
            return (T) valueStatus.getValue();
        }

        return null;
    }

    @Override public ITask<T> block() throws InterruptedException {
        job.join();
        return this;
    }
}
