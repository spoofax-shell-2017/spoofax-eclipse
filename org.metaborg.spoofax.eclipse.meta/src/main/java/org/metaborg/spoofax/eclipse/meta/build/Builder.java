package org.metaborg.spoofax.eclipse.meta.build;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.project.IProject;
import org.metaborg.core.project.IProjectService;
import org.metaborg.meta.core.project.ILanguageSpec;
import org.metaborg.meta.core.project.ILanguageSpecService;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.meta.core.LanguageSpecBuildInput;
import org.metaborg.spoofax.meta.core.config.ISpoofaxLanguageSpecConfig;
import org.metaborg.spoofax.meta.core.config.ISpoofaxLanguageSpecConfigService;
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpecPaths;
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpecPathsService;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

public abstract class Builder extends IncrementalProjectBuilder {
    private static final ILogger logger = LoggerUtils.logger(Builder.class);

    private final IEclipseResourceService resourceService;
    private final IProjectService projectService;
    private final ILanguageSpecService languageSpecService;
    private final ISpoofaxLanguageSpecConfigService configService;
    private final ISpoofaxLanguageSpecPathsService pathsService;

    public Builder(IEclipseResourceService resourceService, IProjectService projectService, ILanguageSpecService languageSpecService, ISpoofaxLanguageSpecConfigService configService, ISpoofaxLanguageSpecPathsService pathsService) {
        this.resourceService = resourceService;
        this.projectService = projectService;
        this.languageSpecService = languageSpecService;
        this.configService = configService;
        this.pathsService = pathsService;

    }


    @Override protected final org.eclipse.core.resources.IProject[] build(int kind, Map<String, String> args,
        IProgressMonitor monitor) throws CoreException {
        if(kind == AUTO_BUILD) {
            return null;
        }

        try {
            final ILanguageSpec languageSpec = languageSpec();
            if(languageSpec == null) {
                logger.error("Cannot {} language project; cannot retrieve Metaborg project for {}", description(),
                    getProject());
                monitor.setCanceled(true);
                return null;
            }

            try {
                build(languageSpec, monitor);
            } catch(OperationCanceledException e) {
                // Ignore
            } catch(Exception e) {
                monitor.setCanceled(true);
                logger.error("Cannot {} language project {}; build failed unexpectedly", e, description(), languageSpec);
            }
            return null;
        } finally {
            // Always forget last build state to force a full build next time.
            forgetLastBuiltState();
        }
    }

    @Override protected final void clean(IProgressMonitor monitor) throws CoreException {
        try {
            final ILanguageSpec languageSpec = languageSpec();
//            final IProject project = project();
            if(languageSpec == null) {
                logger.error("Cannot clean language project; cannot retrieve Metaborg project for {}", getProject());
                monitor.setCanceled(true);
                return;
            }

            try {
                clean(languageSpec, monitor);
            } catch(OperationCanceledException e) {
                // Ignore
            } catch(Exception e) {
                monitor.setCanceled(true);
                logger.error("Cannot clean language project {}; build failed unexpectedly", e, languageSpec);
            }
        } finally {
            // Always forget last build state to force a full build next time.
            forgetLastBuiltState();
        }
    }

    protected LanguageSpecBuildInput createBuildInput(ILanguageSpec languageSpec) throws IOException {
        final ISpoofaxLanguageSpecConfig config = this.configService.get(languageSpec);
        final ISpoofaxLanguageSpecPaths paths = this.pathsService.get(languageSpec);
        return new LanguageSpecBuildInput(languageSpec, config, paths);
    }


    private ILanguageSpec languageSpec() {
        final org.eclipse.core.resources.IProject eclipseProject = getProject();
        final FileObject location = resourceService.resolve(eclipseProject);
        final IProject project = projectService.get(location);
        final ILanguageSpec languageSpec = languageSpecService.get(project);
        return languageSpec;
    }


    protected abstract void build(ILanguageSpec languageSpec, IProgressMonitor monitor) throws CoreException, IOException;

    protected abstract void clean(ILanguageSpec languageSpec, IProgressMonitor monitor) throws CoreException, IOException;

    protected abstract String description();
}
