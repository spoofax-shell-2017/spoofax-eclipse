package org.metaborg.spoofax.eclipse.project;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.resources.IResource;
import org.metaborg.core.config.ConfigRequest;
import org.metaborg.core.config.IProjectConfig;
import org.metaborg.core.config.IProjectConfigService;
import org.metaborg.core.messages.StreamMessagePrinter;
import org.metaborg.core.project.IProject;
import org.metaborg.core.source.ISourceTextService;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.inject.Inject;

public class EclipseProjectService implements IEclipseProjectService {
    private static final ILogger logger = LoggerUtils.logger(EclipseProjectService.class);

    private final ISourceTextService sourceTextService;
    private final IEclipseResourceService resourceService;
    private final IProjectConfigService projectConfigService;


    @Inject public EclipseProjectService(ISourceTextService sourceTextService, IEclipseResourceService resourceService,
        IProjectConfigService projectConfigService) {
        this.sourceTextService = sourceTextService;
        this.resourceService = resourceService;
        this.projectConfigService = projectConfigService;
    }


    @Override public IEclipseProject get(FileObject resource) {
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

        final ConfigRequest<? extends IProjectConfig> configRequest = projectConfigService.get(location);
        if(!configRequest.valid()) {
            logger.error("Errors occurred when retrieving project configuration from project directory {}", location);
            configRequest.reportErrors(new StreamMessagePrinter(sourceTextService, false, false, logger));
            return null;
        }

        final IProjectConfig config = configRequest.config();

        return new EclipseProject(location, config, eclipseProject);
    }


    @Override public IEclipseProject get(IProject project) {
        if(project instanceof IEclipseProject) {
            return (IEclipseProject) project;
        }
        return get(project.location());
    }
}
