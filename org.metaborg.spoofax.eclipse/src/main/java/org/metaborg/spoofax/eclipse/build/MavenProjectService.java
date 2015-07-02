package org.metaborg.spoofax.eclipse.build;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.metaborg.core.project.IMavenProjectService;
import org.metaborg.core.project.IProject;
import org.metaborg.spoofax.eclipse.resource.EclipseProject;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;

import com.google.inject.Inject;

public class MavenProjectService implements IMavenProjectService {
    private final IEclipseResourceService resourceService;
    private final IMavenProjectRegistry mavenProjectRegistry;


    @Inject public MavenProjectService(IEclipseResourceService resourceService) {
        this.resourceService = resourceService;
        this.mavenProjectRegistry = MavenPlugin.getMavenProjectRegistry();
    }


    @Override public MavenProject get(IProject project) {
        MavenProject mavenProject = getFromProject(project);
        if(mavenProject != null) {
            return mavenProject;
        }

        mavenProject = getFromPom(project);
        if(mavenProject != null) {
            return mavenProject;
        }

        return null;
    }

    private MavenProject getFromProject(IProject project) {
        try {
            if(project instanceof EclipseProject) {
                final EclipseProject eclipseProject = (EclipseProject) project;
                final IMavenProjectFacade mavenProjectFacade =
                    mavenProjectRegistry.getProject(eclipseProject.eclipseProject);
                if(mavenProjectFacade != null) {
                    return mavenProjectFacade.getMavenProject(null);
                }
            }
        } catch(CoreException e) {

        }
        return null;
    }

    private MavenProject getFromPom(IProject project) {
        try {
            final FileObject pomResource = project.location().resolveFile("pom.xml");
            if(!pomResource.exists()) {
                return null;
            }

            final IResource pomEclipseResource = resourceService.unresolve(pomResource);
            if(pomEclipseResource == null) {
                return null;
            }

            if(pomEclipseResource instanceof IFile) {
                final IFile pomEclipseFile = (IFile) pomEclipseResource;

                final IMavenProjectFacade mavenProjectFacade = mavenProjectRegistry.create(pomEclipseFile, true, null);
                if(mavenProjectFacade != null) {
                    return mavenProjectFacade.getMavenProject(null);
                }
            }
        } catch(FileSystemException | CoreException e) {

        }
        return null;
    }
}
