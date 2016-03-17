package org.metaborg.spoofax.eclipse.language;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.metaborg.core.language.LanguageComponentChange;
import org.metaborg.core.processing.ILanguageChangeProcessor;
import org.metaborg.spoofax.eclipse.processing.ProgressReporter;
import org.metaborg.spoofax.eclipse.util.StatusUtils;

/**
 * Job for processing language changes.
 */
public class LanguageComponentChangeJob extends Job {
    private final ILanguageChangeProcessor processor;
    private final LanguageComponentChange change;


    public LanguageComponentChangeJob(ILanguageChangeProcessor processor, LanguageComponentChange change) {
        super("Processing language component " + change.kind.toString());

        this.processor = processor;
        this.change = change;
    }


    @Override protected IStatus run(IProgressMonitor monitor) {
        processor.processComponentChange(change, new ProgressReporter(monitor));
        return StatusUtils.success();
    }
}
