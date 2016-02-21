package org.metaborg.spoofax.eclipse.resource;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.resources.IResource;
import org.metaborg.core.config.ConfigException;
import org.metaborg.core.config.IProjectConfig;
import org.metaborg.core.config.IProjectConfigService;
import org.metaborg.core.project.IProject;
import org.metaborg.core.project.IProjectService;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.inject.Inject;

public class EclipseProjectService implements IProjectService {
    private static final ILogger logger = LoggerUtils.logger(EclipseProjectService.class);

    private final IEclipseResourceService resourceService;
    private final IProjectConfigService projectConfigService;


    @Inject public EclipseProjectService(IEclipseResourceService resourceService,
        IProjectConfigService projectConfigService) {
        this.resourceService = resourceService;
        this.projectConfigService = projectConfigService;
    }


    @Override public IProject get(FileObject resource) {
        final IResource eclipseResource = resourceService.unresolve(resource);
        if(eclipseResource == null) {
            logger.debug("Cannot get project, {} is not an Eclipse resource, or does not exist any more", resource);
            return null;
        }

        final org.eclipse.core.resources.IProject eclipseProject = eclipseResource.getProject();
        if(eclipseProject == null) {
            logger.error("Cannot get project, {} is the Eclipse workspace root", resource);
            return null;
        }

        final FileObject location = resourceService.resolve(eclipseProject);

        final IProjectConfig config;
        try {
            config = projectConfigService.get(location);
        } catch(ConfigException e) {
            logger.error("Cannot get project for {}, configuration could not be retrieved", e, resource);
            return null;
        }

        return new EclipseProject(location, config, eclipseProject);
    }
}
