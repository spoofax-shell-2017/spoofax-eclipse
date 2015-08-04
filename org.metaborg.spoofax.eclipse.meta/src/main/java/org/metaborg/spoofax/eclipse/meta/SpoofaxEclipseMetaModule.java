package org.metaborg.spoofax.eclipse.meta;

import org.metaborg.spoofax.eclipse.meta.ant.EclipseAntRunnerService;
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
}
