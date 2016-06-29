package org.metaborg.spoofax.eclipse.meta.language;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.metaborg.spoofax.eclipse.util.StatusUtils;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

/**
 * Job for discovering languages and dialects from plugins.
 */
public class DiscoverDialectsFromProjectsJob extends Job {
    private static final ILogger logger = LoggerUtils.logger(DiscoverDialectsFromProjectsJob.class);

    private final MetaLanguageLoader metaLanguageLoader;


    public DiscoverDialectsFromProjectsJob(MetaLanguageLoader metaLanguageLoader) {
        super("Loading all Spoofax dialects from open projects");
        setPriority(Job.LONG);

        this.metaLanguageLoader = metaLanguageLoader;
    }


    @Override protected IStatus run(IProgressMonitor monitor) {
        logger.debug("Loading all Spoofax dialects from open projects");
        metaLanguageLoader.loadDialectsFromProjects();
        return StatusUtils.success();
    }
}
