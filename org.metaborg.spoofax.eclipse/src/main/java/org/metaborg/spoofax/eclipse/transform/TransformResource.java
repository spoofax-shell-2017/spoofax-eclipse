package org.metaborg.spoofax.eclipse.transform;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.project.IProject;

public class TransformResource {
    public final FileObject resource;
    public final IProject project;
    public final String text;


    public TransformResource(FileObject resource, IProject project, String text) {
        this.resource = resource;
        this.project = project;
        this.text = text;
    }


    @Override public String toString() {
        return resource.toString();
    }
}
