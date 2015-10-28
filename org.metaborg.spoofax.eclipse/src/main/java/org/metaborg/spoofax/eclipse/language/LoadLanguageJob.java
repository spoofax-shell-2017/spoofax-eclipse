package org.metaborg.spoofax.eclipse.language;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.metaborg.spoofax.eclipse.util.StatusUtils;

public class LoadLanguageJob extends Job {
    private final EclipseLanguageLoader discoverer;

    private final FileObject location;
    private final boolean skipUnavailable;


    public LoadLanguageJob(EclipseLanguageLoader discoverer, FileObject location, boolean skipUnavailable) {
        super("Loading Spoofax language");
        setPriority(Job.SHORT);

        this.discoverer = discoverer;

        this.location = location;
        this.skipUnavailable = skipUnavailable;
    }


    @Override protected IStatus run(IProgressMonitor monitor) {
        discoverer.load(location, skipUnavailable);
        return StatusUtils.success();
    }
}
