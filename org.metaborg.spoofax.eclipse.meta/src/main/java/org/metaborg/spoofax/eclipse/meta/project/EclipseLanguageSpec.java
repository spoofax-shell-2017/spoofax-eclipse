package org.metaborg.spoofax.eclipse.meta.project;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.spoofax.eclipse.project.EclipseProject;
import org.metaborg.spoofax.meta.core.config.ISpoofaxLanguageSpecConfig;
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpec;

public class EclipseLanguageSpec implements ISpoofaxLanguageSpec {
    private final EclipseProject project;
    private final ISpoofaxLanguageSpecConfig config;


    public EclipseLanguageSpec(EclipseProject project, ISpoofaxLanguageSpecConfig config) {
        this.project = project;
        this.config = config;
    }

    @Override public FileObject location() {
        return project.location();
    }

    @Override public ISpoofaxLanguageSpecConfig config() {
        return config;
    }

}
