package org.metaborg.spoofax.eclipse.meta.bootstrap;

import org.metaborg.core.MetaborgException;
import org.metaborg.core.config.ConfigException;
import org.metaborg.spoofax.meta.core.config.ISpoofaxLanguageSpecConfig;
import org.metaborg.spoofax.meta.core.config.ISpoofaxLanguageSpecConfigBuilder;
import org.metaborg.spoofax.meta.core.config.ISpoofaxLanguageSpecConfigWriter;
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpec;
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpecService;

import com.google.inject.Provider;

public abstract class BootstrapConfigChange implements IBootstrapChange {
    protected final BootstrapProject project;

    protected final ISpoofaxLanguageSpecConfigWriter languageSpecConfigWriter;
    protected final ISpoofaxLanguageSpecService languageSpecService;
    protected final Provider<ISpoofaxLanguageSpecConfigBuilder> languageSpecConfigBuilderProvider;


    public BootstrapConfigChange(BootstrapProject project, ISpoofaxLanguageSpecConfigWriter languageSpecConfigWriter,
        ISpoofaxLanguageSpecService languageSpecService,
        Provider<ISpoofaxLanguageSpecConfigBuilder> languageSpecConfigBuilderProvider) {
        this.project = project;

        this.languageSpecConfigWriter = languageSpecConfigWriter;
        this.languageSpecService = languageSpecService;
        this.languageSpecConfigBuilderProvider = languageSpecConfigBuilderProvider;
    }


    @Override public abstract void apply() throws MetaborgException;

    @Override abstract public void unapply() throws MetaborgException;


    protected void updateConfig(BootstrapProject project, ISpoofaxLanguageSpecConfigBuilder languageSpecConfigBuilder)
        throws ConfigException {
        // Write configuration file to disk.
        final ISpoofaxLanguageSpecConfig newConfig = languageSpecConfigBuilder.build(project.location());
        languageSpecConfigWriter.write(project.languageSpec(), newConfig, null);

        // Update bootstrap project with new language specification project.
        final ISpoofaxLanguageSpec newLanguageSpec = languageSpecService.get(project.languageSpec());
        project.updateLanguageSpec(newLanguageSpec);
    }
}
