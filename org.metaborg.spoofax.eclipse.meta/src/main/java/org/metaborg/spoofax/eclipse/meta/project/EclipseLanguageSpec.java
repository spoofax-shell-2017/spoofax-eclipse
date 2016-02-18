package org.metaborg.spoofax.eclipse.meta.project;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.resources.IProject;
import org.metaborg.spoofax.eclipse.resource.EclipseProject;
import org.metaborg.spoofax.meta.core.config.ISpoofaxLanguageSpecConfig;
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpec;
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpecPaths;

public class EclipseLanguageSpec extends EclipseProject implements ISpoofaxLanguageSpec {
    private final ISpoofaxLanguageSpecConfig config;
    private final ISpoofaxLanguageSpecPaths paths;

    
    public EclipseLanguageSpec(ISpoofaxLanguageSpecConfig config, ISpoofaxLanguageSpecPaths paths, FileObject location,
        IProject eclipseProject) {
        super(location, eclipseProject);
        this.config = config;
        this.paths = paths;
    }


    @Override public ISpoofaxLanguageSpecConfig config() {
        return config;
    }

    @Override public ISpoofaxLanguageSpecPaths paths() {
        return paths;
    }
}
