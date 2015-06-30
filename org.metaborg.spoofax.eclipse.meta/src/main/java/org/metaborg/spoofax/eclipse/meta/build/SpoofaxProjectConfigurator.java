package org.metaborg.spoofax.eclipse.meta.build;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.metaborg.spoofax.eclipse.meta.nature.SpoofaxMetaNature;

public class SpoofaxProjectConfigurator extends AbstractProjectConfigurator {
    @Override public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
        final IProject project = request.getProject();
        SpoofaxMetaNature.add(project);
    }
}
