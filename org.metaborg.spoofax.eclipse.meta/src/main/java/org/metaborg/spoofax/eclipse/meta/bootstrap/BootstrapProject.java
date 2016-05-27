package org.metaborg.spoofax.eclipse.meta.bootstrap;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.language.LanguageIdentifier;
import org.metaborg.spoofax.eclipse.util.Nullable;
import org.metaborg.spoofax.meta.core.build.LangSpecCommonPaths;
import org.metaborg.spoofax.meta.core.config.ISpoofaxLanguageSpecConfig;
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpec;

public class BootstrapProject {
    public final org.eclipse.core.resources.IProject eclipseProject;

    private ISpoofaxLanguageSpec languageSpec;
    private @Nullable FileObject binary;



    public BootstrapProject(org.eclipse.core.resources.IProject eclipseProject, ISpoofaxLanguageSpec languageSpec) {
        this.eclipseProject = eclipseProject;
        this.languageSpec = languageSpec;
    }


    public static String toNoVersionId(LanguageIdentifier identifier) {
        return identifier.groupId + ":" + identifier.id;
    }


    public ISpoofaxLanguageSpec languageSpec() {
        return languageSpec;
    }

    public FileObject location() {
        return languageSpec.location();
    }

    public ISpoofaxLanguageSpecConfig config() {
        return languageSpec.config();
    }

    public LanguageIdentifier identifier() {
        return config().identifier();
    }

    public String idNoVersion() {
        return toNoVersionId(identifier());
    }

    public LangSpecCommonPaths paths() {
        return new LangSpecCommonPaths(location());
    }

    public @Nullable FileObject binary() {
        return binary;
    }


    public void updateLanguageSpec(ISpoofaxLanguageSpec languageSpec) {
        this.languageSpec = languageSpec;
    }

    public void updateBinary(FileObject binary) {
        this.binary = binary;
    }


    @Override public int hashCode() {
        return idNoVersion().hashCode();
    }

    @Override public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }
        if(obj == null) {
            return false;
        }
        if(getClass() != obj.getClass()) {
            return false;
        }
        final BootstrapProject other = (BootstrapProject) obj;
        if(!idNoVersion().equals(other.idNoVersion())) {
            return false;
        }
        return true;
    }

    @Override public String toString() {
        return idNoVersion();
    }
}
