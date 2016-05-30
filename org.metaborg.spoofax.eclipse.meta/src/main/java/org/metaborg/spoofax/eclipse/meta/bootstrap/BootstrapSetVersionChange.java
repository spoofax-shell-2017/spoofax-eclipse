package org.metaborg.spoofax.eclipse.meta.bootstrap;

import org.metaborg.core.config.ConfigException;
import org.metaborg.core.language.LanguageIdentifier;
import org.metaborg.core.language.LanguageVersion;
import org.metaborg.spoofax.eclipse.util.Nullable;
import org.metaborg.spoofax.meta.core.config.ISpoofaxLanguageSpecConfig;
import org.metaborg.spoofax.meta.core.config.ISpoofaxLanguageSpecConfigBuilder;
import org.metaborg.spoofax.meta.core.config.ISpoofaxLanguageSpecConfigWriter;
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpecService;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.inject.Provider;

public class BootstrapSetVersionChange extends BootstrapConfigChange {
    private static final ILogger logger = LoggerUtils.logger(BootstrapSetVersionChange.class);

    private final LanguageVersion newVersion;

    private @Nullable LanguageVersion prevVersion;


    public BootstrapSetVersionChange(BootstrapProject project,
        ISpoofaxLanguageSpecConfigWriter languageSpecConfigWriter, ISpoofaxLanguageSpecService languageSpecService,
        Provider<ISpoofaxLanguageSpecConfigBuilder> languageSpecConfigBuilderProvider, LanguageVersion newVersion) {
        super(project, languageSpecConfigWriter, languageSpecService, languageSpecConfigBuilderProvider);
        this.newVersion = newVersion;
    }


    @Override public void apply() throws ConfigException {
        prevVersion = project.config().identifier().version;

        logger.info("Setting version of {} to {}", project, newVersion);
        setVersion(newVersion);
    }

    @Override public void unapply() throws ConfigException {
        if(prevVersion == null) {
            return;
        }

        logger.info("Reverting version of {} from {} to {}", project, newVersion, prevVersion);
        setVersion(prevVersion);

        prevVersion = null;
    }


    private void setVersion(LanguageVersion version) throws ConfigException {
        final ISpoofaxLanguageSpecConfig config = project.config();
        final ISpoofaxLanguageSpecConfigBuilder languageSpecConfigBuilder = languageSpecConfigBuilderProvider.get();
        languageSpecConfigBuilder.copyFrom(config);

        // Set version
        final LanguageIdentifier identifier = config.identifier();
        final LanguageIdentifier newIdentifier = new LanguageIdentifier(identifier.groupId, identifier.id, version);
        languageSpecConfigBuilder.withIdentifier(newIdentifier);

        // Update configuration
        updateConfig(project, languageSpecConfigBuilder);
    }
}
