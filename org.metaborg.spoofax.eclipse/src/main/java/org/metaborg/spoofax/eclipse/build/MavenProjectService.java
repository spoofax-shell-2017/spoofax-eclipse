package org.metaborg.spoofax.eclipse.build;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.core.MavenPlugin;
import org.metaborg.spoofax.core.project.IMavenProjectService;
import org.metaborg.spoofax.core.project.IProject;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;

import com.google.inject.Inject;

public class MavenProjectService implements IMavenProjectService {
    private final IEclipseResourceService resourceService;


    @Inject public MavenProjectService(IEclipseResourceService resourceService) {
        this.resourceService = resourceService;
    }


    @Override public MavenProject get(IProject project) {
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
                final MavenProject mavenProject =
                    MavenPlugin.getMavenProjectRegistry().create(pomEclipseFile, true, null).getMavenProject(null);
                return mavenProject;
            }
        } catch(FileSystemException | CoreException e) {
            
        }
        return null;
    }
}
