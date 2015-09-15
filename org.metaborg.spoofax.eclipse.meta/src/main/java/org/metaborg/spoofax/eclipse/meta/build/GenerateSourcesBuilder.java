package org.metaborg.spoofax.eclipse.meta.build;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.project.IProject;
import org.metaborg.core.project.IProjectService;
import org.metaborg.core.project.ProjectException;
import org.metaborg.spoofax.core.project.settings.ISpoofaxProjectSettingsService;
import org.metaborg.spoofax.core.project.settings.SpoofaxProjectSettings;
import org.metaborg.spoofax.eclipse.meta.SpoofaxMetaPlugin;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.meta.core.MetaBuildInput;
import org.metaborg.spoofax.meta.core.SpoofaxMetaBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;

public class GenerateSourcesBuilder extends Builder {
    public static final String id = SpoofaxMetaPlugin.id + ".builder.generatesources";

    private static final Logger logger = LoggerFactory.getLogger(GenerateSourcesBuilder.class);

    private final ISpoofaxProjectSettingsService projectSettingsService;
    private final SpoofaxMetaBuilder builder;


    public GenerateSourcesBuilder() {
        super(SpoofaxMetaPlugin.injector().getInstance(IEclipseResourceService.class), SpoofaxMetaPlugin.injector()
            .getInstance(IProjectService.class));
        final Injector injector = SpoofaxMetaPlugin.injector();
        this.projectSettingsService = injector.getInstance(ISpoofaxProjectSettingsService.class);
        this.builder = injector.getInstance(SpoofaxMetaBuilder.class);
    }


    @Override protected void build(final IProject project, final IProgressMonitor monitor) throws CoreException,
        MetaborgException, ProjectException {
        final SpoofaxProjectSettings settings = projectSettingsService.get(project);
        final MetaBuildInput input = new MetaBuildInput(project, settings);

        final IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
            @Override public void run(IProgressMonitor workspaceMonitor) throws CoreException {
                try {
                    builder.initialize(input);
                    builder.generateSources(input);
                } catch(Exception e) {
                    workspaceMonitor.setCanceled(true);
                    monitor.setCanceled(true);
                    logger.error("Cannot generate sources for language project {}; build failed unexpectedly", e,
                        project);
                }
            }
        };
        ResourcesPlugin.getWorkspace().run(runnable, getProject(), IWorkspace.AVOID_UPDATE, monitor);
    }

    @Override protected void clean(final IProject project, final IProgressMonitor monitor) throws CoreException,
        MetaborgException, ProjectException {
        final SpoofaxProjectSettings settings = projectSettingsService.get(project);
        final MetaBuildInput input = new MetaBuildInput(project, settings);

        final IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
            @Override public void run(IProgressMonitor workspaceMonitor) throws CoreException {
                try {
                    builder.clean(input.settings);
                    builder.initialize(input);
                    builder.generateSources(input);
                } catch(Exception e) {
                    workspaceMonitor.setCanceled(true);
                    monitor.setCanceled(true);
                    logger.error("Cannot clean language project {}; build failed unexpectedly", e, project);
                }
            }
        };
        ResourcesPlugin.getWorkspace().run(runnable, getProject(), IWorkspace.AVOID_UPDATE, monitor);
    }

    @Override protected String description() {
        return "generate sources for";
    }
}
