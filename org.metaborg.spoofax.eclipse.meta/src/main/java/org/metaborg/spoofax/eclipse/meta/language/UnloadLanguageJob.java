package org.metaborg.spoofax.eclipse.meta.language;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.metaborg.core.language.ILanguageComponent;
import org.metaborg.core.language.ILanguageService;
import org.metaborg.spoofax.eclipse.util.StatusUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnloadLanguageJob extends Job {
    private static final Logger logger = LoggerFactory.getLogger(UnloadLanguageJob.class);

    private final ILanguageService languageService;
    private final FileObject location;


    public UnloadLanguageJob(ILanguageService languageService, FileObject location) {
        super("Reloading Spoofax language");
        setPriority(Job.SHORT);

        this.languageService = languageService;
        this.location = location;
    }


    @Override protected IStatus run(IProgressMonitor monitor) {
        final ILanguageComponent component = languageService.getComponent(location.getName());
        if(component != null) {
            languageService.remove(component);
        } else {
            final String message =
                String.format("Failed to unload component at location %s because it does not exist", location);
            logger.error(message);
            return StatusUtils.error(message);
        }
        return StatusUtils.success();
    }
}
