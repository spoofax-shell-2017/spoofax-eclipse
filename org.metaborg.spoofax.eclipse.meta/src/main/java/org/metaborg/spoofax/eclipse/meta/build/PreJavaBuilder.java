package org.metaborg.spoofax.eclipse.meta.build;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.vfs2.FileObject;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.metaborg.spoofax.core.project.IMavenProjectService;
import org.metaborg.spoofax.core.project.IProjectService;
import org.metaborg.spoofax.eclipse.meta.SpoofaxMetaPlugin;
import org.metaborg.spoofax.eclipse.meta.ant.EclipseAntLogger;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.meta.core.MetaBuildInput;
import org.metaborg.spoofax.meta.core.SpoofaxMetaBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;

public class PreJavaBuilder extends IncrementalProjectBuilder {
    public static final String id = SpoofaxMetaPlugin.id + ".builder.prejava";

    private static final Logger logger = LoggerFactory.getLogger(PreJavaBuilder.class);

    private final IEclipseResourceService resourceService;
    private final IProjectService projectService;
    private final IMavenProjectService mavenProjectService;

    private final SpoofaxMetaBuilder builder;


    public PreJavaBuilder() {
        final Injector injector = SpoofaxMetaPlugin.injector();
        this.resourceService = injector.getInstance(IEclipseResourceService.class);
        this.projectService = injector.getInstance(IProjectService.class);
        this.mavenProjectService = injector.getInstance(IMavenProjectService.class);
        this.builder = injector.getInstance(SpoofaxMetaBuilder.class);
    }


    @Override protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor)
        throws CoreException {
        try {
            if(kind != AUTO_BUILD) {
                build(getProject());
            }
        } catch(Exception e) {
            logger.error("Cannot build language project", e);
        } finally {
            // Always forget last build state to force a full build next time.
            forgetLastBuiltState();
        }
        return null;
    }

    @Override protected void clean(IProgressMonitor monitor) throws CoreException {
        try {
            clean(getProject());
        } catch(IOException e) {
            logger.error("Cannot clean language project", e);
        } finally {
            // Always forget last build state to force a full build next time.
            forgetLastBuiltState();
        }
    }

    private void clean(IProject project) throws CoreException, IOException {
        logger.debug("Cleaning language project {}", project);
    }

    private void build(IProject eclipseProject) throws Exception {
        final FileObject location = resourceService.resolve(eclipseProject);
        final org.metaborg.spoofax.core.project.IProject project = projectService.get(location);
        if(project == null) {
            logger.error("Cannot build language project, project for {} could not be retrieved", location);
            return;
        }
        final MavenProject mavenProject = mavenProjectService.get(project);
        if(mavenProject == null) {
            logger.error("Cannot build language project, Maven project for {} could not be retrieved", project);
            return;
        }

        logger.debug("Building language project {}", project);
        final MetaBuildInput input = MetaBuildInput.fromMavenProject(project, mavenProject);
        if(input == null) {
            logger.error("Cannot build language project, build input for {} could not be retrieved", mavenProject);
            return;
        }

        builder.compilePreJava(input, AntClasspathGenerator.classpaths(), new EclipseAntLogger());
    }
}
