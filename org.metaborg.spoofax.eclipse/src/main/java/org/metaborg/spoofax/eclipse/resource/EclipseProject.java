package org.metaborg.spoofax.eclipse.resource;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.project.IProject;

public class EclipseProject implements IProject {
    public final FileObject location;
    public final org.eclipse.core.resources.IProject eclipseProject;


    public EclipseProject(FileObject location, org.eclipse.core.resources.IProject eclipseProject) {
        this.location = location;
        this.eclipseProject = eclipseProject;
    }


    @Override public FileObject location() {
        return location;
    }
    
    
    @Override public String toString() {
        return location.toString();
    }
}
