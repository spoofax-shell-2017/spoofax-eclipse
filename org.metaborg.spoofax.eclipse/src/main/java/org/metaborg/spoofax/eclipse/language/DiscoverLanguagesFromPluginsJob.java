package org.metaborg.spoofax.eclipse.language;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.metaborg.spoofax.eclipse.util.StatusUtils;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

/**
 * Job for discovering languages and dialects from plugins.
 */
public class DiscoverLanguagesFromPluginsJob extends Job {
    private static final ILogger logger = LoggerUtils.logger(DiscoverLanguagesFromPluginsJob.class);

    private final LanguageLoader languageLoader;


    public DiscoverLanguagesFromPluginsJob(LanguageLoader languageLoader) {
        super("Loading all Spoofax languages from plugins");
        setPriority(Job.LONG);
        setSystem(true);

        this.languageLoader = languageLoader;
    }


    @Override protected IStatus run(IProgressMonitor monitor) {
        logger.debug("Loading all Spoofax languages from plugins");
        languageLoader.loadFromPlugins();
        return StatusUtils.success();
    }
}
