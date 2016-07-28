package org.metaborg.spoofax.eclipse.meta.wizard;

import java.io.IOException;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.metaborg.core.config.ConfigException;
import org.metaborg.core.project.IProject;
import org.metaborg.core.project.IProjectService;
import org.metaborg.core.project.ProjectException;
import org.metaborg.spoofax.eclipse.meta.SpoofaxMetaPlugin;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.eclipse.util.AbstractHandlerUtils;
import org.metaborg.spoofax.meta.core.config.ISpoofaxLanguageSpecConfig;
import org.metaborg.spoofax.meta.core.generator.GeneratorSettings;
import org.metaborg.spoofax.meta.core.generator.general.LangProjectGenerator;
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpec;
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpecService;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.inject.Injector;

public class CreateLangProjectHandler extends AbstractHandler {
    private static final ILogger logger = LoggerUtils.logger(CreateLangProjectHandler.class);

    private final IEclipseResourceService resourceService;
    private final IProjectService projectService;
    private final ISpoofaxLanguageSpecService languageSpecService;


    public CreateLangProjectHandler() {
        final Injector injector = SpoofaxMetaPlugin.injector();
        this.resourceService = injector.getInstance(IEclipseResourceService.class);
        this.projectService = injector.getInstance(IProjectService.class);
        this.languageSpecService = injector.getInstance(ISpoofaxLanguageSpecService.class);
    }


    @Override public Object execute(ExecutionEvent event) throws ExecutionException {
        final org.eclipse.core.resources.IProject eclipseProject = AbstractHandlerUtils.toProject(event);
        if(eclipseProject == null) {
            return null;
        }

        final FileObject projectLocation = resourceService.resolve(eclipseProject);
        final IProject project = projectService.get(projectLocation);
        if(project == null) {
            logger.error("Creating example project for {} failed unexpectedly; project could not be retrieved",
                projectLocation);
            return null;
        }

        try {
            final ISpoofaxLanguageSpec langSpec = languageSpecService.get(project);
            final ISpoofaxLanguageSpecConfig config = langSpec.config();
            final FileObject baseDir = resourceService.resolveWorkspaceRoot();
            final FileObject location = LangProjectGenerator.siblingDir(baseDir, config.identifier().id);
            final GeneratorSettings settings = new GeneratorSettings(location, langSpec.config());
            final LangProjectGenerator generator = new LangProjectGenerator(settings);
            generator.generateAll();
        } catch(ProjectException | IOException | ConfigException e) {
            logger.error("Creating example project for {} failed unexpectedly", e, project);
            return null;
        }

        return null;
    }
}
