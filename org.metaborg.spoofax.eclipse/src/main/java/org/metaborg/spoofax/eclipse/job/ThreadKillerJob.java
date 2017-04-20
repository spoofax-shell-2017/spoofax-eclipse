package org.metaborg.spoofax.eclipse.job;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.metaborg.spoofax.eclipse.util.StatusUtils;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

/**
 * Job that interrupts given thread when scheduled, and kills the thread after a certain time.
 */
public class ThreadKillerJob extends Job {
    private static final ILogger logger = LoggerUtils.logger(ThreadKillerJob.class);

    private final Thread thread;
    private final long killTimeMillis;


    public ThreadKillerJob(Thread thread, long killTimeMillis) {
        super("Killing thread");

        this.thread = thread;
        this.killTimeMillis = killTimeMillis;

        setSystem(true);
        setPriority(INTERACTIVE);

    }


    @SuppressWarnings("deprecation") @Override protected IStatus run(IProgressMonitor monitor) {
        if(monitor.isCanceled())
            return StatusUtils.cancel();

        logger.warn("Interrupting {}, killing after {}ms", thread, killTimeMillis);
        thread.interrupt();

        try {
            Thread.sleep(killTimeMillis);
        } catch(InterruptedException e) {
            return StatusUtils.cancel();
        }

        if(monitor.isCanceled())
            return StatusUtils.cancel();

        logger.warn("Killing {}", thread);
        thread.stop();

        return StatusUtils.success();
    }
}
