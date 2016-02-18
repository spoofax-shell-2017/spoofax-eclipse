package org.metaborg.spoofax.eclipse.meta.project;

import java.io.IOException;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.project.IProject;
import org.metaborg.meta.core.config.ILanguageSpecConfig;
import org.metaborg.meta.core.config.ILanguageSpecConfigService;
import org.metaborg.meta.core.project.ILanguageSpec;
import org.metaborg.meta.core.project.ILanguageSpecService;
import org.metaborg.spoofax.eclipse.resource.EclipseProject;
import org.metaborg.spoofax.eclipse.util.Nullable;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.inject.Inject;

public class EclipseLanguageSpecService implements ILanguageSpecService {
    private static final ILogger logger = LoggerUtils.logger(EclipseLanguageSpecService.class);

    private final ILanguageSpecConfigService configService;


    @Inject public EclipseLanguageSpecService(ILanguageSpecConfigService configService) {
        this.configService = configService;
    }


    @Override public boolean available(IProject project) {
        if(project instanceof EclipseLanguageSpec) {
            return true;
        }

        try {
            if(!configService.available(project.location())) {
                return false;
            }
        } catch(IOException e) {
            return false;
        }

        if(!(project instanceof EclipseProject)) {
            return false;
        }

        return true;
    }

    @Override public @Nullable ILanguageSpec get(IProject project) {
        if(project instanceof EclipseLanguageSpec) {
            return (EclipseLanguageSpec) project;
        }

        final FileObject location = project.location();
        final ILanguageSpecConfig config;
        try {
            if(!configService.available(location)) {
                return null;
            }
            config = configService.get(location);
            if(config == null) {
                // Configuration should never be null if it is available, but sanity check anyway.
                return null;
            }
        } catch(IOException e) {
            return null;
        }

        if(!(project instanceof EclipseProject)) {
            logger.warn("Project {} is not an Eclipse project, cannot convert to a language specification project",
                project);
            return null;
        }
        final EclipseProject eclipseProject = (EclipseProject) project;

        return new EclipseLanguageSpec(config, location, eclipseProject.eclipseProject);
    }
}
