package org.metaborg.spoofax.eclipse.meta.build;

import org.eclipse.core.internal.resources.BuildConfiguration;
import org.eclipse.core.resources.IProject;

@SuppressWarnings("restriction")
public class BuilderConfig extends BuildConfiguration {
    public final boolean stopOnFail;
    public final boolean throwOnFail;
    public final boolean reloadLanguage;


    public BuilderConfig(IProject project) {
        this(project, true, false, true);
    }

    public BuilderConfig(IProject project, boolean stopOnFail, boolean throwOnFail, boolean reloadLanguage) {
        super(project);
        this.stopOnFail = stopOnFail;
        this.throwOnFail = throwOnFail;
        this.reloadLanguage = reloadLanguage;
    }
}
