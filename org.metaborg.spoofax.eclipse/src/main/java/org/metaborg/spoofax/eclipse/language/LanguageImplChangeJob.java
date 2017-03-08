package org.metaborg.spoofax.eclipse.language;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.metaborg.core.language.LanguageImplChange;
import org.metaborg.core.processing.ILanguageChangeProcessor;
import org.metaborg.spoofax.eclipse.util.StatusUtils;

/**
 * Job for processing language changes.
 */
public class LanguageImplChangeJob extends Job {
    private final ILanguageChangeProcessor processor;
    private final LanguageImplChange change;


    public LanguageImplChangeJob(ILanguageChangeProcessor processor, LanguageImplChange change) {
        super("Processing language implementation " + change.kind.toString());
        setSystem(true);

        this.processor = processor;
        this.change = change;
    }


    @Override protected IStatus run(IProgressMonitor monitor) {
        processor.processImplChange(change);
        return StatusUtils.success();
    }
}
