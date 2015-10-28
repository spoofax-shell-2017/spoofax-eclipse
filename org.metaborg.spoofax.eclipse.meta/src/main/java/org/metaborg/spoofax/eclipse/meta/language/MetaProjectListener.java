package org.metaborg.spoofax.eclipse.meta.language;

import java.util.Collection;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.metaborg.core.project.IProjectService;
import org.metaborg.core.project.ProjectException;
import org.metaborg.spoofax.core.project.settings.ISpoofaxProjectSettingsService;
import org.metaborg.spoofax.eclipse.meta.nature.AddNatureJob;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

/**
 * Adds the Spoofax meta nature to new projects
 */
public class MetaProjectListener implements IResourceChangeListener {
    private static final ILogger logger = LoggerUtils.logger(MetaProjectListener.class);

    private final IEclipseResourceService resourceService;
    private final IProjectService projectService;
    private final ISpoofaxProjectSettingsService projectSettingsService;


    @Inject public MetaProjectListener(IEclipseResourceService resourceService, IProjectService projectService,
        ISpoofaxProjectSettingsService projectSettingsService) {
        this.resourceService = resourceService;
        this.projectService = projectService;
        this.projectSettingsService = projectSettingsService;
    }


    @Override public void resourceChanged(IResourceChangeEvent event) {
        final Collection<IProject> newProjects = Lists.newLinkedList();

        final IResourceDelta delta = event.getDelta();
        if(delta == null) {
            return;
        }

        try {
            delta.accept(new IResourceDeltaVisitor() {
                public boolean visit(IResourceDelta delta) throws CoreException {
                    final IResource resource = delta.getResource();
                    if(resource instanceof IProject) {
                        final IProject project = (IProject) resource;
                        final int kind = delta.getKind();
                        if(kind == IResourceDelta.ADDED && project.isAccessible()) {
                            newProjects.add(project);
                        }
                    }

                    // Only continue for the workspace root
                    return resource.getType() == IResource.ROOT;
                }
            });
        } catch(CoreException e) {
            logger.error("Error occurred during project added notification", e);
        }

        for(IProject project : newProjects) {
            if(!isMetaLanguageProject(project)) {
                return;
            }
            final AddNatureJob job = new AddNatureJob(project);
            job.schedule();
        }
    }

    public boolean isMetaLanguageProject(IProject eclipseProject) {
        final FileObject resource = resourceService.resolve(eclipseProject);
        final org.metaborg.core.project.IProject project = projectService.get(resource);
        try {
            return projectSettingsService.get(project) != null;
        } catch(ProjectException e) {
            return false;
        }
    }
}
