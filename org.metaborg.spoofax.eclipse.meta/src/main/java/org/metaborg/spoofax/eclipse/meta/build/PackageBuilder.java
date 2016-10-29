package org.metaborg.spoofax.eclipse.meta.build;

import java.io.IOException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.project.IProjectService;
import org.metaborg.spoofax.eclipse.language.LanguageLoader;
import org.metaborg.spoofax.eclipse.meta.SpoofaxMetaPlugin;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.meta.core.build.LanguageSpecBuildInput;
import org.metaborg.spoofax.meta.core.build.LanguageSpecBuilder;
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpec;
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpecService;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.inject.Injector;

public class PackageBuilder extends Builder {
    private static final class PackageRunnable implements IWorkspaceRunnable {
        private final LanguageSpecBuildInput input;
        private final LanguageSpecBuilder builder;

        private boolean success = false;


        private PackageRunnable(LanguageSpecBuildInput input, LanguageSpecBuilder builder) {
            this.input = input;
            this.builder = builder;
        }


        @Override public void run(IProgressMonitor workspaceMonitor) throws CoreException {
            try {
                logger.info("Packaging language project {}", input.languageSpec());
                builder.pkg(input);
                success = true;
            } catch(MetaborgException e) {
                // HACK: disable cancellation for bootstrapping.
                //workspaceMonitor.setCanceled(true);
                //monitor.setCanceled(true);
                if(e.getCause() != null) {
                    logger.error("Exception thrown during packaging", e);
                    logger.error("PACKAGING FAILED");
                } else {
                    final String message = e.getMessage();
                    if(message != null && !message.isEmpty()) {
                        logger.error(message);
                    }
                    logger.error("PACKAGING FAILED");
                }
            }
        }
    }

    private static final class ArchiveRunnable implements IWorkspaceRunnable {
        private final LanguageSpecBuildInput input;
        private final LanguageSpecBuilder builder;

        private boolean success = false;


        private ArchiveRunnable(LanguageSpecBuildInput input, LanguageSpecBuilder builder) {
            this.input = input;
            this.builder = builder;
        }


        @Override public void run(IProgressMonitor workspaceMonitor) throws CoreException {
            try {
                logger.info("Archiving language project {}", input.languageSpec());
                builder.archive(input);
                success = true;
            } catch(MetaborgException e) {
                // HACK: disable cancellation for bootstrapping.
                //workspaceMonitor.setCanceled(true);
                //monitor.setCanceled(true);
                if(e.getCause() != null) {
                    logger.error("Exception thrown during archiving", e);
                    logger.error("ARCHIVING FAILED");
                } else {
                    final String message = e.getMessage();
                    if(message != null && !message.isEmpty()) {
                        logger.error(message);
                    }
                    logger.error("ARCHIVING FAILED");
                }
            }
        }
    }


    public static final String id = SpoofaxMetaPlugin.id + ".builder.postjava";

    private static final ILogger logger = LoggerUtils.logger(PackageBuilder.class);

    private final LanguageLoader discoverer;
    private final LanguageSpecBuilder builder;


    public PackageBuilder() {
        super(SpoofaxMetaPlugin.injector().getInstance(IEclipseResourceService.class),
            SpoofaxMetaPlugin.injector().getInstance(IProjectService.class),
            SpoofaxMetaPlugin.injector().getInstance(ISpoofaxLanguageSpecService.class));
        final Injector injector = SpoofaxMetaPlugin.injector();
        this.discoverer = injector.getInstance(LanguageLoader.class);
        this.builder = injector.getInstance(LanguageSpecBuilder.class);
    }


    @Override protected void build(ISpoofaxLanguageSpec languageSpec, IProgressMonitor monitor)
        throws CoreException, IOException {
        final LanguageSpecBuildInput input = createBuildInput(languageSpec);

        final PackageRunnable packageRunnable = new PackageRunnable(input, builder);
        ResourcesPlugin.getWorkspace().run(packageRunnable, getProject(), IWorkspace.AVOID_UPDATE, monitor);

        if(!packageRunnable.success) {
            failure(monitor);
            return;
        }

        // Refresh in between to sync Eclipse file system with the local file system.
        getProject().refreshLocal(IResource.DEPTH_INFINITE, monitor);
        final ArchiveRunnable archiveRunnable = new ArchiveRunnable(input, builder);
        ResourcesPlugin.getWorkspace().run(archiveRunnable, getProject(), IWorkspace.AVOID_UPDATE, monitor);

        if(!archiveRunnable.success) {
            failure(monitor);
            return;
        }

        final BuilderConfig config = getConfig();
        if(config == null || config.reloadLanguage) {
            // Refresh again to sync file systems.
            getProject().refreshLocal(IResource.DEPTH_INFINITE, monitor);

            logger.info("Reloading language project {}", languageSpec);
            discoverer.loadJob(languageSpec.location(), false).schedule();
        }
    }

    @Override protected void clean(ISpoofaxLanguageSpec languageSpec, IProgressMonitor monitor) {

    }

    @Override protected String description() {
        return "package";
    }
}
