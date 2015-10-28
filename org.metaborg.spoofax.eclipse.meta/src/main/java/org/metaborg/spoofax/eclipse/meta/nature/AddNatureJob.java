package org.metaborg.spoofax.eclipse.meta.nature;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.metaborg.spoofax.eclipse.util.StatusUtils;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

public class AddNatureJob extends Job {
    private static final ILogger logger = LoggerUtils.logger(AddNatureJob.class);

    private final IProject project;


    public AddNatureJob(IProject project) {
        super("Adding Spoofax meta nature");
        
        this.project = project;
    }


    @Override protected IStatus run(IProgressMonitor monitor) {
        try {
            SpoofaxMetaNature.add(project, monitor);
        } catch(CoreException e) {
            logger.error("Could not add Spoofax meta nature to {}", e, project);
            return StatusUtils.silentError();
        }
        return StatusUtils.success();
    }
}
