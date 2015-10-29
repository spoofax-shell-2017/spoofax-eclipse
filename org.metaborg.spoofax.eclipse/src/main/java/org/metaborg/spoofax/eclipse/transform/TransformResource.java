package org.metaborg.spoofax.eclipse.transform;

import org.apache.commons.vfs2.FileObject;

public class TransformResource {
    public final FileObject resource;
    public final String text;


    public TransformResource(FileObject resource, String text) {
        this.resource = resource;
        this.text = text;
    }


    @Override public String toString() {
        return resource.toString();
    }
}
