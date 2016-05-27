package org.metaborg.spoofax.eclipse.meta;

import org.metaborg.meta.core.project.ILanguageSpecService;
import org.metaborg.spoofax.eclipse.meta.ant.EclipseAntRunnerService;
import org.metaborg.spoofax.eclipse.meta.bootstrap.BootstrapJobFactory;
import org.metaborg.spoofax.eclipse.meta.project.EclipseLanguageSpecService;
import org.metaborg.spoofax.meta.core.SpoofaxMetaModule;
import org.metaborg.spoofax.meta.core.ant.IAntRunnerService;
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpecService;

import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;

public class SpoofaxEclipseMetaModule extends SpoofaxMetaModule {
    @Override protected void configure() {
        super.configure();
        
        install(new FactoryModuleBuilder().build(BootstrapJobFactory.class));
    }
    
    /**
     * Overrides {@link SpoofaxMetaModule#bindAnt()} for Eclipse implementation of Ant runner service.
     */
    @Override protected void bindAnt() {
        bind(IAntRunnerService.class).to(EclipseAntRunnerService.class).in(Singleton.class);
    }

    /**
     * Overrides {@link SpoofaxMetaModule#bindLanguageSpec()} for Eclipse implementation of language specification
     * service.
     */
    @Override protected void bindLanguageSpec() {
        bind(EclipseLanguageSpecService.class).in(Singleton.class);
        bind(ILanguageSpecService.class).to(EclipseLanguageSpecService.class);
        bind(ISpoofaxLanguageSpecService.class).to(EclipseLanguageSpecService.class);
    }
}
