package org.metaborg.spoofax.eclipse.meta.build;

import java.util.Map;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.project.IProject;
import org.metaborg.core.project.IProjectService;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

public abstract class Builder extends IncrementalProjectBuilder {
    private static final ILogger logger = LoggerUtils.logger(Builder.class);

    private final IEclipseResourceService resourceService;
    private final IProjectService projectService;


    public Builder(IEclipseResourceService resourceService, IProjectService projectService) {
        this.resourceService = resourceService;
        this.projectService = projectService;
    }


    @Override protected final org.eclipse.core.resources.IProject[] build(int kind, Map<String, String> args,
        IProgressMonitor monitor) throws CoreException {
        if(kind == AUTO_BUILD) {
            return null;
        }

        try {
            final IProject project = project();
            if(project == null) {
                logger.error("Cannot {} language project; cannot retrieve Metaborg project for {}", description(),
                    getProject());
                monitor.setCanceled(true);
                return null;
            }

            try {
                build(project, monitor);
            } catch(Exception e) {
                monitor.setCanceled(true);
                logger.error("Cannot {} language project {}; build failed unexpectedly", e, description(), project);
            }
            return null;
        } finally {
            // Always forget last build state to force a full build next time.
            forgetLastBuiltState();
        }
    }

    @Override protected final void clean(IProgressMonitor monitor) throws CoreException {
        try {
            final IProject project = project();
            if(project == null) {
                logger.error("Cannot clean language project; cannot retrieve Metaborg project for {}", getProject());
                monitor.setCanceled(true);
                return;
            }

            try {
                clean(project, monitor);
            } catch(Exception e) {
                monitor.setCanceled(true);
                logger.error("Cannot clean language project {}; build failed unexpectedly", e, project);
            }
        } finally {
            // Always forget last build state to force a full build next time.
            forgetLastBuiltState();
        }
    }

    private IProject project() {
        final org.eclipse.core.resources.IProject eclipseProject = getProject();
        final FileObject location = resourceService.resolve(eclipseProject);
        final IProject project = projectService.get(location);
        return project;
    }


    protected abstract void build(IProject project, IProgressMonitor monitor) throws CoreException, MetaborgException;

    protected abstract void clean(IProject project, IProgressMonitor monitor) throws CoreException, MetaborgException;

    protected abstract String description();
}
