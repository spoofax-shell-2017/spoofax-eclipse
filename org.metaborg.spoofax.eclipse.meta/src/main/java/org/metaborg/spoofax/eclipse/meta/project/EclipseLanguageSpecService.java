package org.metaborg.spoofax.eclipse.meta.project;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.config.ConfigException;
import org.metaborg.core.config.ConfigRequest;
import org.metaborg.core.messages.StreamMessagePrinter;
import org.metaborg.core.project.IProject;
import org.metaborg.core.source.ISourceTextService;
import org.metaborg.spoofax.eclipse.project.EclipseProject;
import org.metaborg.spoofax.eclipse.util.Nullable;
import org.metaborg.spoofax.meta.core.config.ISpoofaxLanguageSpecConfig;
import org.metaborg.spoofax.meta.core.config.ISpoofaxLanguageSpecConfigService;
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpec;
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpecService;
import org.metaborg.spoofax.meta.core.project.SpoofaxLanguageSpecPaths;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.inject.Inject;

public class EclipseLanguageSpecService implements ISpoofaxLanguageSpecService {
    private static final ILogger logger = LoggerUtils.logger(EclipseLanguageSpecService.class);

    private final ISourceTextService sourceTextService;
    private final ISpoofaxLanguageSpecConfigService configService;


    @Inject public EclipseLanguageSpecService(ISourceTextService sourceTextService,
        ISpoofaxLanguageSpecConfigService configService) {
        this.sourceTextService = sourceTextService;
        this.configService = configService;
    }


    @Override public boolean available(IProject project) {
        if(project instanceof EclipseLanguageSpec) {
            return true;
        }

        if(!configService.available(project.location())) {
            return false;
        }

        if(!(project instanceof EclipseProject)) {
            return false;
        }

        return true;
    }

    @Override public @Nullable ISpoofaxLanguageSpec get(IProject project) throws ConfigException {
        if(project instanceof EclipseLanguageSpec) {
            return (EclipseLanguageSpec) project;
        }

        if(!(project instanceof EclipseProject)) {
            logger.error("Project {} is not an Eclipse project, cannot convert to a language specification project",
                project);
            return null;
        }
        final EclipseProject eclipseProject = (EclipseProject) project;

        final FileObject location = project.location();
        final ConfigRequest<ISpoofaxLanguageSpecConfig> configRequest = configService.get(location);
        if(!configRequest.valid()) {
            logger.error("Errors occurred when retrieving language specification configuration from project {}",
                project);
            configRequest.reportErrors(new StreamMessagePrinter(sourceTextService, false, false, logger));
            return null;
        }

        final ISpoofaxLanguageSpecConfig config = configRequest.config();
        if(config == null) {
            // Configuration should never be null if it is available, but sanity check anyway.
            return null;
        }

        final SpoofaxLanguageSpecPaths paths = new SpoofaxLanguageSpecPaths(location, config);

        return new EclipseLanguageSpec(config, paths, location, eclipseProject.eclipseProject);
    }
}
