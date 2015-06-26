package org.metaborg.spoofax.eclipse.meta;

import org.metaborg.spoofax.eclipse.meta.ant.EclipseAntRunnerService;
import org.metaborg.spoofax.eclipse.meta.build.MetaBuildInputGenerator;
import org.metaborg.spoofax.meta.core.SpoofaxMetaModule;
import org.metaborg.spoofax.meta.core.ant.IAntRunnerService;

import com.google.inject.Singleton;

public class SpoofaxEclipseMetaModule extends SpoofaxMetaModule {
    @Override protected void configure() {
        super.configure();

        bind(IAntRunnerService.class).to(EclipseAntRunnerService.class).in(Singleton.class);

        bind(MetaBuildInputGenerator.class).in(Singleton.class);
    }
}
