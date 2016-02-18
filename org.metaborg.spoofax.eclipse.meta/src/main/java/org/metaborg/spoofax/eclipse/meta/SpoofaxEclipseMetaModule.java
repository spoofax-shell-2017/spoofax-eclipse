package org.metaborg.spoofax.eclipse.meta;

import org.metaborg.meta.core.MetaborgMetaModule;
import org.metaborg.meta.core.project.ILanguageSpecService;
import org.metaborg.spoofax.eclipse.meta.ant.EclipseAntRunnerService;
import org.metaborg.spoofax.eclipse.meta.project.EclipseLanguageSpecService;
import org.metaborg.spoofax.meta.core.SpoofaxMetaModule;
import org.metaborg.spoofax.meta.core.ant.IAntRunnerService;

import com.google.inject.Singleton;

public class SpoofaxEclipseMetaModule extends SpoofaxMetaModule {
    /**
     * Overrides {@link SpoofaxMetaModule#bindAnt()} for Eclipse implementation of Ant runner service.
     */
    @Override protected void bindAnt() {
        bind(IAntRunnerService.class).to(EclipseAntRunnerService.class).in(Singleton.class);
    }

    /**
     * Overrides {@link MetaborgMetaModule#bindLanguageSpec()} for Eclipse implementation of language specification
     * service.
     */
    @Override protected void bindLanguageSpec() {
        bind(ILanguageSpecService.class).to(EclipseLanguageSpecService.class).in(Singleton.class);
    }
}
