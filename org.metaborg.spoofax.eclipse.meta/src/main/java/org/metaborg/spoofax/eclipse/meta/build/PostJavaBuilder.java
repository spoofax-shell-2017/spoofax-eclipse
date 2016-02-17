package org.metaborg.spoofax.eclipse.meta.build;

import com.google.inject.Injector;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.project.IProjectService;
import org.metaborg.meta.core.project.ILanguageSpec;
import org.metaborg.meta.core.project.ILanguageSpecService;
import org.metaborg.spoofax.eclipse.language.EclipseLanguageLoader;
import org.metaborg.spoofax.eclipse.meta.SpoofaxMetaPlugin;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.meta.core.LanguageSpecBuildInput;
import org.metaborg.spoofax.meta.core.SpoofaxMetaBuilder;
import org.metaborg.spoofax.meta.core.config.ISpoofaxLanguageSpecConfigService;
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpecPathsService;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import java.io.IOException;

public class PostJavaBuilder extends Builder {
    private static final class BuildRunnable implements IWorkspaceRunnable {
        private final LanguageSpecBuildInput input;
        private final SpoofaxMetaBuilder builder;
        private final IProgressMonitor monitor;

        private boolean success = false;


        private BuildRunnable(LanguageSpecBuildInput input, SpoofaxMetaBuilder builder, IProgressMonitor monitor) {
            this.input = input;
            this.builder = builder;
            this.monitor = monitor;
        }


        @Override public void run(IProgressMonitor workspaceMonitor) throws CoreException {
            try {
                logger.info("Packaging language project {}", input.languageSpec);
                builder.compilePostJava(input);
                success = true;
            } catch(Exception e) {
                workspaceMonitor.setCanceled(true);
                monitor.setCanceled(true);
                if(e.getCause() != null) {
                    logger.error("Exception thrown during build", e);
                    logger.error("BUILD FAILED");
                } else {
                    final String message = e.getMessage();
                    if(message != null && !message.isEmpty()) {
                        logger.error(message);
                    }
                    logger.error("BUILD FAILED");
                }
            }
        }

        public boolean succeeded() {
            return success;
        }
    }


    public static final String id = SpoofaxMetaPlugin.id + ".builder.postjava";

    private static final ILogger logger = LoggerUtils.logger(PostJavaBuilder.class);

    private final EclipseLanguageLoader discoverer;
    private final SpoofaxMetaBuilder builder;


    public PostJavaBuilder() {
        super(
                SpoofaxMetaPlugin.injector().getInstance(IEclipseResourceService.class),
                SpoofaxMetaPlugin.injector().getInstance(IProjectService.class),
                SpoofaxMetaPlugin.injector().getInstance(ILanguageSpecService.class),
                SpoofaxMetaPlugin.injector().getInstance(ISpoofaxLanguageSpecConfigService.class),
                SpoofaxMetaPlugin.injector().getInstance(ISpoofaxLanguageSpecPathsService.class));
        final Injector injector = SpoofaxMetaPlugin.injector();
        this.discoverer = injector.getInstance(EclipseLanguageLoader.class);
        this.builder = injector.getInstance(SpoofaxMetaBuilder.class);
    }


    @Override protected void build(ILanguageSpec languageSpec, IProgressMonitor monitor) throws CoreException, IOException {
        final LanguageSpecBuildInput input = createBuildInput(languageSpec);

        final BuildRunnable runnable = new BuildRunnable(input, builder, monitor);
        ResourcesPlugin.getWorkspace().run(runnable, getProject(), IWorkspace.AVOID_UPDATE, monitor);

        if(runnable.succeeded()) {
            logger.info("Refreshing language project {}", languageSpec);
            getProject().refreshLocal(IResource.DEPTH_INFINITE, monitor);
            logger.info("Reloading language project {}", languageSpec);
            discoverer.loadJob(languageSpec.location(), false).schedule();
        } else {
            monitor.setCanceled(true);
        }
    }

    @Override protected void clean(ILanguageSpec languageSpec, IProgressMonitor monitor) {

    }

    @Override protected String description() {
        return "package";
    }
}
