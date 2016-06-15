package org.metaborg.spoofax.eclipse.meta.bootstrap;

import java.util.Collection;

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

public class BootstrapSetDepVersionChange extends BootstrapConfigChange {
    private static final ILogger logger = LoggerUtils.logger(BootstrapSetDepVersionChange.class);

    private final String groupId;
    private final String id;
    private final LanguageVersion newVersion;

    private @Nullable LanguageVersion prevSourceVersion;
    private @Nullable LanguageVersion prevCompileVersion;


    public BootstrapSetDepVersionChange(BootstrapProject project,
        ISpoofaxLanguageSpecConfigWriter languageSpecConfigWriter, ISpoofaxLanguageSpecService languageSpecService,
        Provider<ISpoofaxLanguageSpecConfigBuilder> languageSpecConfigBuilderProvider, String groupId, String id,
        LanguageVersion newVersion) {
        super(project, languageSpecConfigWriter, languageSpecService, languageSpecConfigBuilderProvider);
        this.groupId = groupId;
        this.id = id;
        this.newVersion = newVersion;
    }


    @Override public void apply() throws ConfigException {
        logger.info("Setting dependency versions of {}:{} in {} to {}", groupId, id, project, newVersion);

        final ISpoofaxLanguageSpecConfig config = project.config();
        final ISpoofaxLanguageSpecConfigBuilder languageSpecConfigBuilder = languageSpecConfigBuilderProvider.get();
        languageSpecConfigBuilder.copyFrom(config);

        // Set version in source dependencies.
        final Collection<LanguageIdentifier> newSourceDeps = config.sourceDeps();
        for(LanguageIdentifier depId : config.sourceDeps()) {
            if(depId.groupId.equals(groupId) && depId.id.equals(id)) {
                prevSourceVersion = depId.version;
                newSourceDeps.remove(depId);
                newSourceDeps.add(new LanguageIdentifier(depId.groupId, depId.id, newVersion));
                logger.info("  Setting source dependency {}:{} version to {}", groupId, id, newVersion);
                break;
            }
        }
        languageSpecConfigBuilder.withSourceDeps(newSourceDeps);

        // Set version in compile dependencies.
        final Collection<LanguageIdentifier> newCompileDeps = config.compileDeps();
        for(LanguageIdentifier depId : config.compileDeps()) {
            if(depId.groupId.equals(groupId) && depId.id.equals(id)) {
                prevCompileVersion = depId.version;
                newCompileDeps.remove(depId);
                newCompileDeps.add(new LanguageIdentifier(depId.groupId, depId.id, newVersion));
                logger.info("  Setting compile dependency {}:{} version to {}", groupId, id, newVersion);
                break;
            }
        }
        languageSpecConfigBuilder.withCompileDeps(newCompileDeps);

        // Update configuration
        updateConfig(project, languageSpecConfigBuilder);
    }

    @Override public void unapply() throws ConfigException {
        logger.info("Reverting dependency versions of {}:{} in {} from {} to {}/{}", groupId, id, project, newVersion,
            prevSourceVersion, prevCompileVersion);

        final ISpoofaxLanguageSpecConfig config = project.config();
        final ISpoofaxLanguageSpecConfigBuilder languageSpecConfigBuilder = languageSpecConfigBuilderProvider.get();
        languageSpecConfigBuilder.copyFrom(config);

        // Revert version in source dependencies.
        final Collection<LanguageIdentifier> newSourceDeps = config.sourceDeps();
        for(LanguageIdentifier depId : config.sourceDeps()) {
            if(depId.groupId.equals(groupId) && depId.id.equals(id)) {
                newSourceDeps.remove(depId);
                newSourceDeps.add(new LanguageIdentifier(depId.groupId, depId.id, prevSourceVersion));
                logger.info("  Reverting source dependency {}:{} version to {}", groupId, id, prevSourceVersion);
                prevSourceVersion = null;
                break;
            }
        }
        languageSpecConfigBuilder.withSourceDeps(newSourceDeps);

        // Revert version in compile dependencies.
        final Collection<LanguageIdentifier> newCompileDeps = config.compileDeps();
        for(LanguageIdentifier depId : config.compileDeps()) {
            if(depId.groupId.equals(groupId) && depId.id.equals(id)) {
                newCompileDeps.remove(depId);
                newCompileDeps.add(new LanguageIdentifier(depId.groupId, depId.id, prevCompileVersion));
                logger.info("  Reverting compile dependency {}:{} version to {}", groupId, id, prevCompileVersion);
                prevCompileVersion = null;
                break;
            }
        }
        languageSpecConfigBuilder.withCompileDeps(newCompileDeps);

        // Update configuration
        updateConfig(project, languageSpecConfigBuilder);
    }
}
