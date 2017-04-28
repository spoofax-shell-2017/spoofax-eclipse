package org.metaborg.spoofax.eclipse.processing;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.metaborg.core.analysis.IAnalyzeUnit;
import org.metaborg.core.analysis.IAnalyzeUnitUpdate;
import org.metaborg.core.build.BuildInput;
import org.metaborg.core.build.CleanInput;
import org.metaborg.core.build.IBuildOutput;
import org.metaborg.core.build.IBuilder;
import org.metaborg.core.language.LanguageComponentChange;
import org.metaborg.core.language.LanguageImplChange;
import org.metaborg.core.language.dialect.IDialectProcessor;
import org.metaborg.core.processing.ILanguageChangeProcessor;
import org.metaborg.core.processing.IProcessor;
import org.metaborg.core.processing.ITask;
import org.metaborg.core.processing.NullCancel;
import org.metaborg.core.project.IProject;
import org.metaborg.core.resource.ResourceChange;
import org.metaborg.core.syntax.IParseUnit;
import org.metaborg.core.transform.ITransformUnit;
import org.metaborg.spoofax.eclipse.build.BuildRunnable;
import org.metaborg.spoofax.eclipse.build.CleanRunnable;
import org.metaborg.spoofax.eclipse.build.ProcessDialectsRunnable;
import org.metaborg.spoofax.eclipse.job.GlobalSchedulingRules;
import org.metaborg.spoofax.eclipse.language.LanguageComponentChangeJob;
import org.metaborg.spoofax.eclipse.language.LanguageImplChangeJob;
import org.metaborg.spoofax.eclipse.language.LanguageLoader;
import org.metaborg.spoofax.eclipse.project.EclipseProject;
import org.metaborg.spoofax.eclipse.project.IEclipseProjectService;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.eclipse.util.Nullable;
import org.metaborg.util.Ref;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;

import com.google.inject.Inject;

/**
 * Processor implementation that schedules Eclipse workspace runnables and jobs.
 */
public class Processor<P extends IParseUnit, A extends IAnalyzeUnit, AU extends IAnalyzeUnitUpdate, T extends ITransformUnit<?>>
    implements IProcessor<P, A, AU, T> {
    private final IEclipseResourceService resourceService;
    private final IEclipseProjectService projectService;
    private final IDialectProcessor dialectProcessor;
    private final IBuilder<P, A, AU, T> builder;
    private final ILanguageChangeProcessor processor;

    private final GlobalSchedulingRules globalRules;
    private final LanguageLoader languageLoader;

    private final IWorkspace workspace;


    @Inject public Processor(IEclipseResourceService resourceService, IEclipseProjectService projectService,
        IDialectProcessor dialectProcessor, IBuilder<P, A, AU, T> builder, ILanguageChangeProcessor processor,
        GlobalSchedulingRules globalRules, LanguageLoader languageLoader) {
        this.resourceService = resourceService;
        this.projectService = projectService;
        this.dialectProcessor = dialectProcessor;
        this.builder = builder;
        this.processor = processor;

        this.globalRules = globalRules;
        this.languageLoader = languageLoader;

        this.workspace = ResourcesPlugin.getWorkspace();
    }


    @Override public ITask<? extends IBuildOutput<P, A, AU, T>> build(BuildInput input, @Nullable IProgress progress,
        @Nullable ICancel cancel) {
        if(cancel == null) {
            cancel = new NullCancel();
        }
        final Ref<IBuildOutput<P, A, AU, T>> outputRef = new Ref<>();
        final IWorkspaceRunnable runnable =
            new BuildRunnable<>(resourceService, builder, input, progress, cancel, outputRef);
        final IResource projectResource = getResource(input.project);
        final ITask<IBuildOutput<P, A, AU, T>> task =
            new RunnableTask<>(workspace, runnable, projectResource, null, cancel, outputRef, projectResource);
        return task;
    }

    @Override public ITask<?> clean(CleanInput input, @Nullable IProgress progress, @Nullable ICancel cancel) {
        if(cancel == null) {
            cancel = new NullCancel();
        }
        final IWorkspaceRunnable runnable = new CleanRunnable<>(builder, input, progress, cancel);
        final IResource projectResource = getResource(input.project);
        final ITask<?> task =
            new RunnableTask<>(workspace, runnable, projectResource, null, cancel, null, projectResource);
        return task;
    }


    @Override public ITask<?> updateDialects(FileObject location, Iterable<ResourceChange> changes) {
        final ICancel cancel = new NullCancel();
        final IWorkspaceRunnable runnable = new ProcessDialectsRunnable(dialectProcessor, location, changes);
        final IResource projectResource = getResource(location);
        final ITask<?> task = new RunnableTask<>(workspace, runnable, projectResource, null, cancel, null, null);
        return task;
    }


    @Override public ITask<?> languageChange(LanguageComponentChange change) {
        final ICancel cancel = new NullCancel();
        final Job job = new LanguageComponentChangeJob(processor, change);
        job.setRule(new MultiRule(new ISchedulingRule[] { workspace.getRoot(), globalRules.startupReadLock(),
            globalRules.languageServiceLock() }));
        final ITask<?> task = new JobTask<Object>(job, cancel);
        return task;
    }

    @Override public ITask<?> languageChange(LanguageImplChange change) {
        final ICancel cancel = new NullCancel();
        final Job job = new LanguageImplChangeJob(processor, change);
        job.setRule(new MultiRule(new ISchedulingRule[] { workspace.getRoot(), globalRules.startupReadLock(),
            globalRules.languageServiceLock() }));
        final ITask<?> task = new JobTask<Object>(job, cancel);
        return task;
    }


    public void discoverLanguages() {
        languageLoader.loadFromPluginsJob().schedule();
    }


    private @Nullable IResource getResource(IProject project) {
        final EclipseProject eclipseProject = projectService.get(project);
        if(eclipseProject != null) {
            return eclipseProject.eclipseProject;
        }
        return null;
    }

    private @Nullable IResource getResource(FileObject resource) {
        final IResource eclipseResource = resourceService.unresolve(resource);
        return eclipseResource;
    }
}
