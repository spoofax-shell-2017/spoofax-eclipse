package org.metaborg.spoofax.eclipse.transform;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.project.IProject;

public class TransformResource {
    public final FileObject source;
    public final IProject project;
    public final String text;


    public TransformResource(FileObject source, IProject project, String text) {
        this.source = source;
        this.project = project;
        this.text = text;
    }


    @Override public String toString() {
        return source.toString();
    }
}
