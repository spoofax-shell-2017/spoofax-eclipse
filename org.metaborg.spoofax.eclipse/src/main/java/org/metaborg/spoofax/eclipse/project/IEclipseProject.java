package org.metaborg.spoofax.eclipse.project;

import org.metaborg.core.project.IProject;

public interface IEclipseProject extends IProject {
    org.eclipse.core.resources.IProject eclipseProject();
}
