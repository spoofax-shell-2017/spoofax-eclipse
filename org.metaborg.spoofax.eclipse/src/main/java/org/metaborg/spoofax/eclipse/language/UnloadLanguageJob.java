package org.metaborg.spoofax.eclipse.language;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.metaborg.spoofax.eclipse.util.StatusUtils;

public class UnloadLanguageJob extends Job {
    private final LanguageLoader loader;
    private final FileObject location;


    public UnloadLanguageJob(LanguageLoader loader, FileObject location) {
        super("Unloading Spoofax language");
        setPriority(Job.SHORT);

        this.loader = loader;
        this.location = location;
    }


    @Override protected IStatus run(IProgressMonitor monitor) {
        loader.unload(location);
        return StatusUtils.success();
    }
}
