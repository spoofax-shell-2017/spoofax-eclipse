package org.metaborg.spoofax.eclipse.transform;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.project.ILanguageSpec;

public class TransformResource {
    public final ILanguageSpec project;
    public final FileObject resource;
    public final String text;


    public TransformResource(ILanguageSpec project, FileObject resource, String text) {
        this.project = project;
        this.resource = resource;
        this.text = text;
    }


    @Override public String toString() {
        return resource.toString();
    }
}
