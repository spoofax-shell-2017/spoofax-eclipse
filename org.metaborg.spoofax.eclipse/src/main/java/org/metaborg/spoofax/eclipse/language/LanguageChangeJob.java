package org.metaborg.spoofax.eclipse.language;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.metaborg.core.language.LanguageChange;
import org.metaborg.core.processing.ILanguageChangeProcessor;
import org.metaborg.spoofax.eclipse.processing.EclipseProgressReporter;
import org.metaborg.spoofax.eclipse.util.StatusUtils;

/**
 * Job for processing language changes.
 */
public class LanguageChangeJob extends Job {
    private final ILanguageChangeProcessor processor;
    private final LanguageChange change;


    public LanguageChangeJob(ILanguageChangeProcessor processor, LanguageChange change) {
        super("Language change");

        this.processor = processor;
        this.change = change;
    }


    @Override protected IStatus run(IProgressMonitor monitor) {
        processor.process(change, new EclipseProgressReporter(monitor));
        return StatusUtils.success();
    }
}
