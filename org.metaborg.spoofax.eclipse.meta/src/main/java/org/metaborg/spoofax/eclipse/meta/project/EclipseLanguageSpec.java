package org.metaborg.spoofax.eclipse.meta.project;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.resources.IProject;
import org.metaborg.meta.core.config.ILanguageSpecConfig;
import org.metaborg.meta.core.project.ILanguageSpec;
import org.metaborg.spoofax.eclipse.resource.EclipseProject;

public class EclipseLanguageSpec extends EclipseProject implements ILanguageSpec {
    private final ILanguageSpecConfig config;


    public EclipseLanguageSpec(ILanguageSpecConfig config, FileObject location, IProject eclipseProject) {
        super(location, eclipseProject);
        this.config = config;
    }


    @Override public ILanguageSpecConfig config() {
        return config;
    }
}
