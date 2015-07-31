package org.metaborg.spoofax.eclipse.meta.build;

import java.util.Map;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.language.ILanguageDiscoveryService;
import org.metaborg.core.project.IProjectService;
import org.metaborg.core.project.ProjectException;
import org.metaborg.spoofax.core.project.settings.ISpoofaxProjectSettingsService;
import org.metaborg.spoofax.core.project.settings.SpoofaxProjectSettings;
import org.metaborg.spoofax.eclipse.job.GlobalSchedulingRules;
import org.metaborg.spoofax.eclipse.meta.SpoofaxMetaPlugin;
import org.metaborg.spoofax.eclipse.meta.ant.EclipseAntLogger;
import org.metaborg.spoofax.eclipse.meta.language.LoadLanguageJob;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.eclipse.util.StatusUtils;
import org.metaborg.spoofax.meta.core.MetaBuildInput;
import org.metaborg.spoofax.meta.core.SpoofaxMetaBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;

public class PostJavaBuilder extends IncrementalProjectBuilder {
    public static final String id = SpoofaxMetaPlugin.id + ".builder.postjava";

    private static final Logger logger = LoggerFactory.getLogger(PostJavaBuilder.class);

    private final IEclipseResourceService resourceService;
    private final ILanguageDiscoveryService languageDiscoveryService;
    private final IProjectService projectService;
    private final ISpoofaxProjectSettingsService projectSettingsService;

    private final SpoofaxMetaBuilder builder;
    private final GlobalSchedulingRules globalSchedulingRules;


    public PostJavaBuilder() {
        final Injector injector = SpoofaxMetaPlugin.injector();
        this.resourceService = injector.getInstance(IEclipseResourceService.class);
        this.projectService = injector.getInstance(IProjectService.class);
        this.projectSettingsService = injector.getInstance(ISpoofaxProjectSettingsService.class);
        this.languageDiscoveryService = injector.getInstance(ILanguageDiscoveryService.class);
        this.builder = injector.getInstance(SpoofaxMetaBuilder.class);
        this.globalSchedulingRules = injector.getInstance(GlobalSchedulingRules.class);
    }


    @Override protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor)
        throws CoreException {
        try {
            if(kind != AUTO_BUILD) {
                build(getProject(), monitor);
            }
        } catch(Exception e) {
            logger.error("Cannot build language project", e);
        } finally {
            // Always forget last build state to force a full build next time.
            forgetLastBuiltState();
        }
        return null;
    }

    private void build(IProject eclipseProject, IProgressMonitor monitor) throws CoreException, MetaborgException,
        ProjectException {
        final FileObject location = resourceService.resolve(eclipseProject);
        final org.metaborg.core.project.IProject project = projectService.get(location);
        if(project == null) {
            throw new MetaborgException("Cannot get metaborg project for " + eclipseProject);
        }
        final SpoofaxProjectSettings settings = projectSettingsService.get(project);
        final MetaBuildInput input = new MetaBuildInput(project, settings);

        final IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
            @Override public void run(IProgressMonitor workspaceMonitor) throws CoreException {
                try {
                    builder.compilePostJava(input, AntClasspathGenerator.classpaths(), new EclipseAntLogger());
                } catch(Exception e) {
                    throw new CoreException(StatusUtils.error(e));
                }
            }
        };
        ResourcesPlugin.getWorkspace().run(runnable, eclipseProject, IWorkspace.AVOID_UPDATE, monitor);

        final Job languageLoadJob = new LoadLanguageJob(languageDiscoveryService, location);
        languageLoadJob.setRule(new MultiRule(new ISchedulingRule[] { globalSchedulingRules.startupReadLock(),
            globalSchedulingRules.languageServiceLock() }));
        languageLoadJob.schedule();
    }
}
