package org.metaborg.spoofax.eclipse.processing;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.metaborg.core.build.BuildInput;
import org.metaborg.core.build.CleanInput;
import org.metaborg.core.build.IBuildOutput;
import org.metaborg.core.language.ILanguageDiscoveryService;
import org.metaborg.core.language.LanguageChange;
import org.metaborg.core.language.dialect.IDialectProcessor;
import org.metaborg.core.processing.CancellationToken;
import org.metaborg.core.processing.ICancellationToken;
import org.metaborg.core.processing.ILanguageChangeProcessor;
import org.metaborg.core.processing.IProgressReporter;
import org.metaborg.core.processing.ITask;
import org.metaborg.core.project.IProjectService;
import org.metaborg.core.resource.ResourceChange;
import org.metaborg.spoofax.core.build.ISpoofaxBuilder;
import org.metaborg.spoofax.core.processing.ISpoofaxProcessor;
import org.metaborg.spoofax.eclipse.build.BuildRunnable;
import org.metaborg.spoofax.eclipse.build.CleanRunnable;
import org.metaborg.spoofax.eclipse.build.ProcessDialectsRunnable;
import org.metaborg.spoofax.eclipse.job.GlobalSchedulingRules;
import org.metaborg.spoofax.eclipse.language.DiscoverLanguagesJob;
import org.metaborg.spoofax.eclipse.language.LanguageChangeJob;
import org.metaborg.spoofax.eclipse.resource.EclipseProject;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.eclipse.util.Nullable;
import org.metaborg.util.Ref;
import org.spoofax.interpreter.terms.IStrategoTerm;

import com.google.inject.Inject;

/**
 * Processor implementation that schedules Eclipse workspace runnables and jobs.
 */
public class EclipseProcessor implements ISpoofaxProcessor {
    private final IEclipseResourceService resourceService;
    private final ILanguageDiscoveryService languageDiscoveryService;
    private final IDialectProcessor dialectProcessor;
    private final IProjectService projectService;
    private final ISpoofaxBuilder builder;
    private final ILanguageChangeProcessor processor;

    private final GlobalSchedulingRules globalRules;

    private final IWorkspace workspace;



    @Inject public EclipseProcessor(IEclipseResourceService resourceService,
        ILanguageDiscoveryService languageDiscoveryService, IDialectProcessor dialectProcessor,
        IProjectService projectService, ISpoofaxBuilder builder, ILanguageChangeProcessor processor,
        GlobalSchedulingRules globalRules) {
        this.resourceService = resourceService;
        this.languageDiscoveryService = languageDiscoveryService;
        this.dialectProcessor = dialectProcessor;
        this.projectService = projectService;
        this.builder = builder;
        this.processor = processor;

        this.globalRules = globalRules;

        this.workspace = ResourcesPlugin.getWorkspace();
    }


    @Override public ITask<IBuildOutput<IStrategoTerm, IStrategoTerm, IStrategoTerm>> build(BuildInput input,
        @Nullable IProgressReporter progressReporter, @Nullable ICancellationToken cancellationToken) {
        if(cancellationToken == null) {
            cancellationToken = new CancellationToken();
        }
        final Ref<IBuildOutput<IStrategoTerm, IStrategoTerm, IStrategoTerm>> outputRef = new Ref<>();
        final IWorkspaceRunnable runnable =
            new BuildRunnable<>(resourceService, builder, input, progressReporter, cancellationToken, outputRef);
        final ITask<IBuildOutput<IStrategoTerm, IStrategoTerm, IStrategoTerm>> task =
            new RunnableTask<>(workspace, runnable, getProject(input.project), null, cancellationToken, outputRef);
        return task;
    }

    @Override public ITask<?> clean(CleanInput input, @Nullable IProgressReporter progressReporter,
        @Nullable ICancellationToken cancellationToken) {
        if(cancellationToken == null) {
            cancellationToken = new CancellationToken();
        }
        final IWorkspaceRunnable runnable = new CleanRunnable<>(builder, input, progressReporter, cancellationToken);
        final ITask<?> task =
            new RunnableTask<>(workspace, runnable, getProject(input.project), null, cancellationToken, null);
        return task;
    }


    @Override public ITask<?> updateDialects(org.metaborg.core.project.IProject project,
        Iterable<ResourceChange> changes) {
        final CancellationToken cancellationToken = new CancellationToken();
        final IWorkspaceRunnable runnable =
            new ProcessDialectsRunnable(dialectProcessor, project, changes, null, cancellationToken);
        final ITask<?> task =
            new RunnableTask<>(workspace, runnable, getProject(project), null, cancellationToken, null);
        return task;
    }


    @Override public ITask<?> languageChange(LanguageChange change) {
        final CancellationToken cancellationToken = new CancellationToken();
        final Job job = new LanguageChangeJob(processor, change);
        job.setRule(new MultiRule(new ISchedulingRule[] { workspace.getRoot(), globalRules.startupReadLock(),
            globalRules.languageServiceLock() }));
        final ITask<?> task = new JobTask<Object>(job, cancellationToken);
        return task;
    }


    public void discoverLanguages() {
        final Job job =
            new DiscoverLanguagesJob(resourceService, languageDiscoveryService, projectService, dialectProcessor);
        job.setRule(new MultiRule(new ISchedulingRule[] { workspace.getRoot(), globalRules.startupWriteLock(),
            globalRules.languageServiceLock() }));
        job.schedule();
    }


    private IProject getProject(org.metaborg.core.project.IProject project) {
        final EclipseProject eclipseProject = (EclipseProject) project;
        return eclipseProject.eclipseProject;
    }
}
