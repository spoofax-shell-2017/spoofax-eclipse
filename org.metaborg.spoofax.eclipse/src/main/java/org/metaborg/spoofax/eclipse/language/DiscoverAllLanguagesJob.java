package org.metaborg.spoofax.eclipse.language;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.metaborg.spoofax.eclipse.util.StatusUtils;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

/**
 * Job for discovering languages at startup.
 */
public class DiscoverAllLanguagesJob extends Job {
    private static final ILogger logger = LoggerUtils.logger(DiscoverAllLanguagesJob.class);

    private final EclipseLanguageLoader languageDiscoverer;


    public DiscoverAllLanguagesJob(EclipseLanguageLoader projectListener) {
        super("Loading all Spoofax languages");
        setPriority(Job.LONG);

        this.languageDiscoverer = projectListener;
    }


    @Override protected IStatus run(IProgressMonitor monitor) {
        logger.debug("Running discover languages job");
        languageDiscoverer.loadAll();
        return StatusUtils.success();
    }
}
