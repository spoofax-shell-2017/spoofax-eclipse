package org.metaborg.spoofax.eclipse.meta.build;

import java.io.IOException;
import java.util.Map;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.metaborg.spoofax.eclipse.meta.SpoofaxMetaPlugin;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.meta.core.SpoofaxMetaBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;

public class SpoofaxPreJavaMetaProjectBuilder extends IncrementalProjectBuilder {
    public static final String id = SpoofaxMetaPlugin.id + ".builder.prejava";

    private static final Logger logger = LoggerFactory.getLogger(SpoofaxPreJavaMetaProjectBuilder.class);

    private final IEclipseResourceService resourceService;

    private final SpoofaxMetaBuilder builder;


    public SpoofaxPreJavaMetaProjectBuilder() {
        final Injector injector = SpoofaxMetaPlugin.injector();
        this.resourceService = injector.getInstance(IEclipseResourceService.class);
        this.builder = injector.getInstance(SpoofaxMetaBuilder.class);
    }


    @Override protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor)
        throws CoreException {
        try {
            if(kind != AUTO_BUILD) {
                build(getProject(), monitor);
            }
        } catch(IOException e) {
            logger.error("Cannot build language project", e);
        } finally {
            // Always forget last build state to force a full build next time.
            forgetLastBuiltState();
        }
        return null;
    }

    @Override protected void clean(IProgressMonitor monitor) throws CoreException {
        try {
            clean(getProject(), monitor);
        } catch(IOException e) {
            logger.error("Cannot clean language project", e);
        } finally {
            // Always forget last build state to force a full build next time.
            forgetLastBuiltState();
        }
    }

    private void clean(IProject project, IProgressMonitor monitor) throws CoreException, IOException {
        logger.debug("Cleaning language project {}", project);
    }

    private void build(IProject project, IProgressMonitor monitor) throws CoreException, IOException {
        logger.debug("Building language project {}", project);
    }
}
