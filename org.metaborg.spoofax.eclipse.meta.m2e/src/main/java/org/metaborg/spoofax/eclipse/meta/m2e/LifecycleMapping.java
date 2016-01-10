package org.metaborg.spoofax.eclipse.meta.m2e;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.project.configurator.AbstractCustomizableLifecycleMapping;
import org.eclipse.m2e.core.project.configurator.AbstractLifecycleMapping;
import org.eclipse.m2e.core.project.configurator.ILifecycleMapping;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.metaborg.spoofax.eclipse.meta.nature.AddNatureJob;
import org.metaborg.spoofax.eclipse.meta.nature.RemoveNatureJob;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

public class LifecycleMapping extends AbstractCustomizableLifecycleMapping implements ILifecycleMapping {
    private static final ILogger logger = LoggerUtils.logger(AbstractLifecycleMapping.class);


    @Override public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor)
        throws CoreException {
        super.configure(request, monitor);
        final IProject project = request.getProject();
        logger.info("Detected Spoofax language project; adding Spoofax meta-nature to {}", project);
        final AddNatureJob job = new AddNatureJob(project);
        job.setProgressGroup(monitor, 1);
        job.schedule();
    }

    @Override public void unconfigure(ProjectConfigurationRequest request, IProgressMonitor monitor)
        throws CoreException {
        super.unconfigure(request, monitor);
        final IProject project = request.getProject();
        logger.info("Detected Spoofax language project; removing Spoofax meta-nature from {}", project);
        final RemoveNatureJob job = new RemoveNatureJob(project);
        job.setProgressGroup(monitor, 1);
        job.schedule();
    }
}
