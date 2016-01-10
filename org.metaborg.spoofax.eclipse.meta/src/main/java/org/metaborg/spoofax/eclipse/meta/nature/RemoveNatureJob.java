package org.metaborg.spoofax.eclipse.meta.nature;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.metaborg.spoofax.eclipse.util.StatusUtils;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

public class RemoveNatureJob extends Job {
    private static final ILogger logger = LoggerUtils.logger(RemoveNatureJob.class);

    private final IProject project;


    public RemoveNatureJob(IProject project) {
        super("Removing Spoofax meta-nature");

        this.project = project;
    }


    @Override protected IStatus run(IProgressMonitor monitor) {
        try {
            SpoofaxMetaNature.remove(project, monitor);
        } catch(CoreException e) {
            final String message = logger.format("Could not remove Spoofax meta nature from {}", project);
            logger.error(message, e);
            return StatusUtils.silentError(message, e);
        }
        return StatusUtils.success();
    }
}
