package org.metaborg.spoofax.eclipse.meta.build;

import org.eclipse.core.internal.resources.BuildConfiguration;
import org.eclipse.core.resources.IProject;

@SuppressWarnings("restriction")
public class BuilderConfig extends BuildConfiguration {
    public final boolean reloadLanguage;


    public BuilderConfig(IProject project) {
        this(project, true);
    }

    public BuilderConfig(IProject project, boolean reloadLanguage) {
        super(project);
        this.reloadLanguage = reloadLanguage;
    }
}
