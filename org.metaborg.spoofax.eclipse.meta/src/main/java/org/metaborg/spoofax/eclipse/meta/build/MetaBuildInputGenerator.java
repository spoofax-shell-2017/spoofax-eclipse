package org.metaborg.spoofax.eclipse.meta.build;

import org.apache.commons.vfs2.FileObject;
import org.apache.maven.project.MavenProject;
import org.metaborg.core.project.IMavenProjectService;
import org.metaborg.core.project.IProjectService;
import org.metaborg.spoofax.generator.project.ProjectException;
import org.metaborg.spoofax.meta.core.MetaBuildInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class MetaBuildInputGenerator {
    private static final Logger logger = LoggerFactory.getLogger(MetaBuildInputGenerator.class);

    private final IProjectService projectService;
    private final IMavenProjectService mavenProjectService;


    @Inject public MetaBuildInputGenerator(IProjectService projectService, IMavenProjectService mavenProjectService) {
        this.projectService = projectService;
        this.mavenProjectService = mavenProjectService;
    }


    public MetaBuildInput buildInput(FileObject location) {
        final org.metaborg.core.project.IProject project = projectService.get(location);
        if(project == null) {
            logger.error("Cannot build language project, project for {} could not be retrieved", location);
            return null;
        }

        final MavenProject mavenProject = mavenProjectService.get(project);
        if(mavenProject == null) {
            logger.error("Cannot build language project, Maven project for {} could not be retrieved", project);
            return null;
        }

        try {
            final MetaBuildInput input = MetaBuildInput.fromMavenProject(project, mavenProject);
            if(input == null) {
                logger.error("Cannot build language project, build input for {} could not be retrieved", mavenProject);
                return null;
            }

            return input;
        } catch(ProjectException e) {
            logger.error("Cannot build language project, build input for {} could not be retrieved", mavenProject);
            return null;
        }
    }
}
