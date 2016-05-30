package org.metaborg.spoofax.eclipse.meta.build;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.metaborg.core.config.ConfigException;
import org.metaborg.core.project.IProject;
import org.metaborg.core.project.IProjectService;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.eclipse.util.Nullable;
import org.metaborg.spoofax.eclipse.util.StatusUtils;
import org.metaborg.spoofax.meta.core.build.LanguageSpecBuildInput;
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpec;
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpecService;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

public abstract class Builder extends IncrementalProjectBuilder {
    private static final ILogger logger = LoggerUtils.logger(Builder.class);

    private final IEclipseResourceService resourceService;
    private final IProjectService projectService;
    private final ISpoofaxLanguageSpecService languageSpecService;


    public Builder(IEclipseResourceService resourceService, IProjectService projectService,
        ISpoofaxLanguageSpecService languageSpecService) {
        this.resourceService = resourceService;
        this.projectService = projectService;
        this.languageSpecService = languageSpecService;
    }


    @Override protected final org.eclipse.core.resources.IProject[] build(int kind, Map<String, String> args,
        IProgressMonitor monitor) throws CoreException {
        if(kind == AUTO_BUILD) {
            return null;
        }

        try {
            final ISpoofaxLanguageSpec languageSpec = languageSpec();
            if(languageSpec == null) {
                failure(monitor, "Cannot {} language project; cannot retrieve Metaborg project for {}", description(),
                    getProject());
                return null;
            }

            try {
                build(languageSpec, monitor);
            } catch(OperationCanceledException e) {
                // Ignore
            } catch(Exception e) {
                failure(monitor, "Cannot {} language project {}; build failed unexpectedly", e, description(),
                    languageSpec);
            }
        } catch(ConfigException e) {
            failure(monitor, "Cannot get language specification project for project {}", e, getProject());
        } finally {
            // Always forget last build state to force a full build next time.
            forgetLastBuiltState();
        }
        return null;
    }

    @Override protected final void clean(IProgressMonitor monitor) throws CoreException {
        try {
            final ISpoofaxLanguageSpec languageSpec = languageSpec();
            // final IProject project = project();
            if(languageSpec == null) {
                failure(monitor, "Cannot clean language project; cannot retrieve Metaborg project for {}",
                    getProject());
                return;
            }

            try {
                clean(languageSpec, monitor);
            } catch(OperationCanceledException e) {
                // Ignore
            } catch(Exception e) {
                failure(monitor, "Cannot clean language project {}; build failed unexpectedly", e, languageSpec);
            }
        } catch(ConfigException e) {
            failure(monitor, "Cannot get language specification project for project {}", e, getProject());
        } finally {
            // Always forget last build state to force a full build next time.
            forgetLastBuiltState();
        }
    }

    protected LanguageSpecBuildInput createBuildInput(ISpoofaxLanguageSpec languageSpec) throws IOException {
        return new LanguageSpecBuildInput(languageSpec);
    }

    protected @Nullable BuilderConfig getConfig() {
        final IBuildConfiguration config = getBuildConfig();
        if(config instanceof BuilderConfig) {
            return (BuilderConfig) config;
        }
        return null;
    }


    protected void failure(IProgressMonitor monitor) throws CoreException {
        failure(monitor, null);
    }

    protected void failure(IProgressMonitor monitor, @Nullable String fmt, Object... objs) throws CoreException {
        failure(monitor, fmt, null, objs);
    }

    protected void failure(IProgressMonitor monitor, @Nullable String fmt, @Nullable Throwable e, Object... objs)
        throws CoreException {
        final @Nullable String msg;
        if(fmt != null) {
            msg = logger.format(fmt, objs);
            if(e != null) {
                logger.error(msg, e);
            } else {
                logger.error(msg);
            }
        } else {
            msg = null;
        }

        BuilderConfig config = getConfig();
        if(config == null) {
            config = new BuilderConfig(getProject());
        }

        if(config.stopOnFail) {
            monitor.setCanceled(true);
        }
        if(config.throwOnFail) {
            final IStatus status;
            if(fmt != null) {
                if(e != null) {
                    status = StatusUtils.buildFailure(msg, e);
                } else {
                    status = StatusUtils.buildFailure(msg);
                }
            } else {
                status = StatusUtils.buildFailure();
            }
            throw new CoreException(status);
        }
    }


    private ISpoofaxLanguageSpec languageSpec() throws ConfigException {
        final org.eclipse.core.resources.IProject eclipseProject = getProject();
        final FileObject location = resourceService.resolve(eclipseProject);
        final IProject project = projectService.get(location);
        final ISpoofaxLanguageSpec languageSpec = languageSpecService.get(project);
        return languageSpec;
    }


    protected abstract void build(ISpoofaxLanguageSpec languageSpec, IProgressMonitor monitor)
        throws CoreException, IOException;

    protected abstract void clean(ISpoofaxLanguageSpec languageSpec, IProgressMonitor monitor)
        throws CoreException, IOException;

    protected abstract String description();
}
