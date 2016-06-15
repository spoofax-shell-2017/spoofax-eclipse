package org.metaborg.spoofax.eclipse.meta.bootstrap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;

public interface BootstrapJobFactory {
    BootstrapJob create(IWorkspaceRoot workspaceRoot, Iterable<IProject> targetEclipseProjects);
}
