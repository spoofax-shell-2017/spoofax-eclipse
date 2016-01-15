package org.metaborg.spoofax.eclipse.processing;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.metaborg.core.build.BuildInput;
import org.metaborg.core.build.CleanInput;
import org.metaborg.core.build.IBuildOutput;
import org.metaborg.core.language.LanguageComponentChange;
import org.metaborg.core.language.LanguageImplChange;
import org.metaborg.core.language.dialect.IDialectProcessor;
import org.metaborg.core.processing.CancellationToken;
import org.metaborg.core.processing.ICancellationToken;
import org.metaborg.core.processing.ILanguageChangeProcessor;
import org.metaborg.core.processing.IProgressReporter;
import org.metaborg.core.processing.ITask;
import org.metaborg.core.project.ILanguageSpec;
import org.metaborg.core.resource.ResourceChange;
import org.metaborg.spoofax.core.build.ISpoofaxBuilder;
import org.metaborg.spoofax.core.processing.ISpoofaxProcessor;
import org.metaborg.spoofax.eclipse.build.BuildRunnable;
import org.metaborg.spoofax.eclipse.build.CleanRunnable;
import org.metaborg.spoofax.eclipse.build.ProcessDialectsRunnable;
import org.metaborg.spoofax.eclipse.job.GlobalSchedulingRules;
import org.metaborg.spoofax.eclipse.language.DiscoverAllLanguagesJob;
import org.metaborg.spoofax.eclipse.language.EclipseLanguageLoader;
import org.metaborg.spoofax.eclipse.language.LanguageComponentChangeJob;
import org.metaborg.spoofax.eclipse.language.LanguageImplChangeJob;
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
    private final IDialectProcessor dialectProcessor;
    private final ISpoofaxBuilder builder;
    private final ILanguageChangeProcessor processor;

    private final GlobalSchedulingRules globalRules;
    private final EclipseLanguageLoader languageDiscoverer;

    private final IWorkspace workspace;



    @Inject public EclipseProcessor(IEclipseResourceService resourceService, IDialectProcessor dialectProcessor,
        ISpoofaxBuilder builder, ILanguageChangeProcessor processor, GlobalSchedulingRules globalRules,
        EclipseLanguageLoader languageDiscoverer) {
        this.resourceService = resourceService;
        this.dialectProcessor = dialectProcessor;
        this.builder = builder;
        this.processor = processor;

        this.globalRules = globalRules;
        this.languageDiscoverer = languageDiscoverer;

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
            new RunnableTask<>(workspace, runnable, getResource(input.languageSpec), null, cancellationToken, outputRef);
        return task;
    }

    @Override public ITask<?> clean(CleanInput input, @Nullable IProgressReporter progressReporter,
        @Nullable ICancellationToken cancellationToken) {
        if(cancellationToken == null) {
            cancellationToken = new CancellationToken();
        }
        final IWorkspaceRunnable runnable = new CleanRunnable<>(builder, input, progressReporter, cancellationToken);
        final ITask<?> task =
            new RunnableTask<>(workspace, runnable, getResource(input.languageSpec), null, cancellationToken, null);
        return task;
    }


    @Override public ITask<?> updateDialects(FileObject location, Iterable<ResourceChange> changes) {
        final CancellationToken cancellationToken = new CancellationToken();
        final IWorkspaceRunnable runnable =
            new ProcessDialectsRunnable(dialectProcessor, location, changes, null, cancellationToken);
        final ITask<?> task =
            new RunnableTask<>(workspace, runnable, getResource(location), null, cancellationToken, null);
        return task;
    }


    @Override public ITask<?> languageChange(LanguageComponentChange change) {
        final CancellationToken cancellationToken = new CancellationToken();
        final Job job = new LanguageComponentChangeJob(processor, change);
        job.setRule(new MultiRule(new ISchedulingRule[] { workspace.getRoot(), globalRules.startupReadLock(),
            globalRules.languageServiceLock() }));
        final ITask<?> task = new JobTask<Object>(job, cancellationToken);
        return task;
    }

    @Override public ITask<?> languageChange(LanguageImplChange change) {
        final CancellationToken cancellationToken = new CancellationToken();
        final Job job = new LanguageImplChangeJob(processor, change);
        job.setRule(new MultiRule(new ISchedulingRule[] { workspace.getRoot(), globalRules.startupReadLock(),
            globalRules.languageServiceLock() }));
        final ITask<?> task = new JobTask<Object>(job, cancellationToken);
        return task;
    }


    public void discoverLanguages() {
        final Job job = new DiscoverAllLanguagesJob(languageDiscoverer);
        job.setRule(new MultiRule(new ISchedulingRule[] { workspace.getRoot(), globalRules.startupWriteLock(),
            globalRules.languageServiceLock() }));
        job.schedule();
        workspace.addResourceChangeListener(languageDiscoverer);
        // GTODO: remove resource change listener on plugin stop
    }

    private IResource getResource(ILanguageSpec languageSpec) {
        final EclipseProject eclipseProject = (EclipseProject) languageSpec;
        return eclipseProject.eclipseProject;
    }

//    private IResource getResource(org.metaborg.core.project.IProject project) {
//        final EclipseProject eclipseProject = (EclipseProject) project;
//        return eclipseProject.eclipseProject;
//    }

    private @Nullable IResource getResource(FileObject resource) {
        final IResource eclipseResource = resourceService.unresolve(resource);
        return eclipseResource;
    }
}
