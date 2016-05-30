package org.metaborg.spoofax.eclipse.meta.build;

import java.io.IOException;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.metaborg.core.project.IProjectService;
import org.metaborg.spoofax.eclipse.meta.SpoofaxMetaPlugin;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.meta.core.build.LanguageSpecBuildInput;
import org.metaborg.spoofax.meta.core.build.LanguageSpecBuilder;
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpec;
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpecService;
import org.metaborg.util.file.CollectionFileAccess;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.inject.Injector;

public class GenerateSourcesBuilder extends Builder {
    public static final String id = SpoofaxMetaPlugin.id + ".builder.generatesources";

    private static final ILogger logger = LoggerUtils.logger(GenerateSourcesBuilder.class);

    private final LanguageSpecBuilder builder;


    public GenerateSourcesBuilder() {
        super(SpoofaxMetaPlugin.injector().getInstance(IEclipseResourceService.class),
            SpoofaxMetaPlugin.injector().getInstance(IProjectService.class),
            SpoofaxMetaPlugin.injector().getInstance(ISpoofaxLanguageSpecService.class));
        final Injector injector = SpoofaxMetaPlugin.injector();
        this.builder = injector.getInstance(LanguageSpecBuilder.class);
    }


    @Override protected void build(final ISpoofaxLanguageSpec languageSpec, final IProgressMonitor monitor)
        throws CoreException, IOException {
        final LanguageSpecBuildInput input = createBuildInput(languageSpec);

        final IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
            @Override public void run(IProgressMonitor workspaceMonitor) throws CoreException {
                try {
                    logger.info("Generating sources for language project {}", languageSpec);
                    builder.initialize(input);
                    builder.generateSources(input, new CollectionFileAccess());
                } catch(Exception e) {
                    failure(workspaceMonitor,
                        "Cannot generate sources for language project {}; build failed unexpectedly", e, languageSpec);
                }
            }
        };
        ResourcesPlugin.getWorkspace().run(runnable, getProject(), IWorkspace.AVOID_UPDATE, monitor);
    }

    @Override protected void clean(final ISpoofaxLanguageSpec languageSpec, final IProgressMonitor monitor)
        throws CoreException, IOException {
        final LanguageSpecBuildInput input = createBuildInput(languageSpec);

        final IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
            @Override public void run(IProgressMonitor workspaceMonitor) throws CoreException {
                try {
                    logger.info("Cleaning and generating sources for language project {}", languageSpec);
                    builder.clean(input);
                    builder.initialize(input);
                    builder.generateSources(input, new CollectionFileAccess());
                } catch(Exception e) {
                    failure(workspaceMonitor, "Cannot clean language project {}; build failed unexpectedly", e,
                        languageSpec);
                }
            }
        };
        ResourcesPlugin.getWorkspace().run(runnable, getProject(), IWorkspace.AVOID_UPDATE, monitor);
    }

    @Override protected String description() {
        return "generate sources for";
    }
}
