package org.metaborg.spoofax.eclipse.meta.build;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.language.ILanguageDiscoveryService;
import org.metaborg.core.project.IProject;
import org.metaborg.core.project.IProjectService;
import org.metaborg.spoofax.core.project.settings.ISpoofaxProjectSettingsService;
import org.metaborg.spoofax.core.project.settings.SpoofaxProjectSettings;
import org.metaborg.spoofax.eclipse.job.GlobalSchedulingRules;
import org.metaborg.spoofax.eclipse.meta.SpoofaxMetaPlugin;
import org.metaborg.spoofax.eclipse.meta.ant.AntClasspathGenerator;
import org.metaborg.spoofax.eclipse.meta.language.LoadLanguageJob;
import org.metaborg.spoofax.eclipse.processing.EclipseCancellationToken;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.meta.core.MetaBuildInput;
import org.metaborg.spoofax.meta.core.SpoofaxMetaBuilder;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.inject.Injector;

public class PostJavaBuilder extends Builder {
    private static final class BuildRunnable implements IWorkspaceRunnable {
        private final MetaBuildInput input;
        private final SpoofaxMetaBuilder builder;
        private final IProgressMonitor monitor;

        private boolean success = false;


        private BuildRunnable(MetaBuildInput input, SpoofaxMetaBuilder builder, IProgressMonitor monitor) {
            this.input = input;
            this.builder = builder;
            this.monitor = monitor;
        }


        @Override public void run(IProgressMonitor workspaceMonitor) throws CoreException {
            try {
                logger.info("Packaging language project {}", input.project);
                builder.compilePostJava(input, AntClasspathGenerator.classpaths(), null, new EclipseCancellationToken(
                    monitor));
                success = true;
            } catch(Exception e) {
                workspaceMonitor.setCanceled(true);
                monitor.setCanceled(true);
                logger.error("Packaging language project {} failed unexpectedly", e, input.project);
            }
        }

        public boolean succeeded() {
            return success;
        }
    }


    public static final String id = SpoofaxMetaPlugin.id + ".builder.postjava";

    private static final ILogger logger = LoggerUtils.logger(PostJavaBuilder.class);

    private final ILanguageDiscoveryService languageDiscoveryService;
    private final ISpoofaxProjectSettingsService projectSettingsService;

    private final SpoofaxMetaBuilder builder;
    private final GlobalSchedulingRules globalSchedulingRules;


    public PostJavaBuilder() {
        super(SpoofaxMetaPlugin.injector().getInstance(IEclipseResourceService.class), SpoofaxMetaPlugin.injector()
            .getInstance(IProjectService.class));
        final Injector injector = SpoofaxMetaPlugin.injector();
        this.projectSettingsService = injector.getInstance(ISpoofaxProjectSettingsService.class);
        this.languageDiscoveryService = injector.getInstance(ILanguageDiscoveryService.class);
        this.builder = injector.getInstance(SpoofaxMetaBuilder.class);
        this.globalSchedulingRules = injector.getInstance(GlobalSchedulingRules.class);
    }


    @Override protected void build(IProject project, IProgressMonitor monitor) throws CoreException, MetaborgException {
        final SpoofaxProjectSettings settings = projectSettingsService.get(project);
        final MetaBuildInput input = new MetaBuildInput(project, settings);

        final BuildRunnable runnable = new BuildRunnable(input, builder, monitor);
        ResourcesPlugin.getWorkspace().run(runnable, getProject(), IWorkspace.AVOID_UPDATE, monitor);

        if(runnable.succeeded()) {
            logger.info("Reloading language project {}", project);
            final Job languageLoadJob = new LoadLanguageJob(languageDiscoveryService, project.location());
            languageLoadJob.setRule(new MultiRule(new ISchedulingRule[] { globalSchedulingRules.startupReadLock(),
                globalSchedulingRules.languageServiceLock() }));
            languageLoadJob.schedule();
        } else {
            monitor.setCanceled(true);
        }
    }

    @Override protected void clean(IProject project, IProgressMonitor monitor) {

    }

    @Override protected String description() {
        return "package";
    }
}
