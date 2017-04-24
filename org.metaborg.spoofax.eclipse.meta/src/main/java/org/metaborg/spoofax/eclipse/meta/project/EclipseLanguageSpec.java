package org.metaborg.spoofax.eclipse.meta.project;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.spoofax.eclipse.project.EclipseProject;
import org.metaborg.spoofax.meta.core.config.ISpoofaxLanguageSpecConfig;
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpec;

public class EclipseLanguageSpec extends EclipseProject implements ISpoofaxLanguageSpec {
    private final ISpoofaxLanguageSpecConfig config;


    public EclipseLanguageSpec(FileObject location, ISpoofaxLanguageSpecConfig config,
            org.eclipse.core.resources.IProject eclipseProject) {
        super(location, config, eclipseProject);
        this.config = config;
    }

    @Override public ISpoofaxLanguageSpecConfig config() {
        return config;
    }

}
