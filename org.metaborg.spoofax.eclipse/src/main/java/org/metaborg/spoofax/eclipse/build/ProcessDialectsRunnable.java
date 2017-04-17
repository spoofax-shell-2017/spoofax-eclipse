package org.metaborg.spoofax.eclipse.build;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.metaborg.core.language.dialect.IDialectProcessor;
import org.metaborg.core.resource.ResourceChange;

public class ProcessDialectsRunnable implements IWorkspaceRunnable {
    private final IDialectProcessor dialectProcessor;
    private final FileObject location;
    private final Iterable<ResourceChange> changes;


    public ProcessDialectsRunnable(IDialectProcessor dialectProcessor, FileObject location,
        Iterable<ResourceChange> changes) {
        this.dialectProcessor = dialectProcessor;
        this.location = location;
        this.changes = changes;
    }

    @Override public void run(IProgressMonitor monitor) throws CoreException {
        dialectProcessor.update(location, changes);
    }
}
