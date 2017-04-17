package org.metaborg.spoofax.eclipse.processing;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.metaborg.core.processing.ITask;
import org.metaborg.spoofax.eclipse.util.Nullable;
import org.metaborg.util.Ref;
import org.metaborg.util.task.ICancel;

/**
 * Task implementation for Eclipse workspace runnables.
 */
public class RunnableTask<T> implements ITask<T> {
    private final IWorkspace workspace;

    private final IWorkspaceRunnable runnable;
    private final @Nullable ISchedulingRule rule;
    private final @Nullable IProgressMonitor monitor;
    private final ICancel cancel;
    private final @Nullable Ref<T> valueRef;
    private final @Nullable IResource refreshResource;

    private boolean completed = false;


    public RunnableTask(IWorkspace workspace, IWorkspaceRunnable runnable, @Nullable ISchedulingRule rule,
        @Nullable IProgressMonitor monitor, ICancel cancel, @Nullable Ref<T> valueRef,
        @Nullable IResource refreshResource) {
        this.workspace = workspace;
        this.runnable = runnable;
        this.rule = rule;
        this.monitor = monitor;
        this.cancel = cancel;
        this.valueRef = valueRef;
        this.refreshResource = refreshResource;
    }


    @Override public ITask<T> schedule() {
        try {
            workspace.run(runnable, rule, IWorkspace.AVOID_UPDATE, monitor);
            if(refreshResource != null) {
                refreshResource.refreshLocal(IResource.DEPTH_INFINITE, monitor);
            }
        } catch(CoreException e) {

        }
        completed = true;
        return this;
    }

    @Override public void cancel() {
        cancel.cancel();
    }

    @Override public void cancel(int forceTimeout) {
        cancel.cancel();
        // TODO: kill after timeout
    }

    @Override public boolean completed() {
        return completed;
    }

    @Override public boolean cancelled() {
        return cancel.cancelled();
    }

    @Override public T result() {
        if(valueRef == null) {
            return null;
        }
        return valueRef.get();
    }

    @Override public ITask<T> block() throws InterruptedException {
        // Does nothing, schedule already blocks.
        return this;
    }
}
