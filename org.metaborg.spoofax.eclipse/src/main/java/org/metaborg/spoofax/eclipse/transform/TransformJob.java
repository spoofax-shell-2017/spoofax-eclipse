package org.metaborg.spoofax.eclipse.transform;

import java.io.IOException;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.metaborg.core.action.ITransformGoal;
import org.metaborg.core.analysis.AnalysisFileResult;
import org.metaborg.core.context.ContextException;
import org.metaborg.core.context.IContext;
import org.metaborg.core.context.IContextService;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.processing.analyze.IAnalysisResultRequester;
import org.metaborg.core.processing.parse.IParseResultRequester;
import org.metaborg.core.project.ILanguageSpec;
import org.metaborg.core.syntax.ParseResult;
import org.metaborg.core.transform.ITransformService;
import org.metaborg.core.transform.TransformException;
import org.metaborg.spoofax.eclipse.job.ThreadKillerJob;
import org.metaborg.spoofax.eclipse.util.StatusUtils;
import org.metaborg.util.concurrent.IClosableLock;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;

public class TransformJob<P, A, T> extends Job {
    private static final ILogger logger = LoggerUtils.logger(TransformJob.class);
    private static final long interruptTimeMillis = 3000;
    private static final long killTimeMillis = 5000;

    private final IContextService contextService;
    private final ITransformService<P, A, T> transformService;

    private final IParseResultRequester<P> parseResultRequester;
    private final IAnalysisResultRequester<P, A> analysisResultRequester;

    private final ILanguageImpl language;
    private final Iterable<TransformResource> resources;
    private final ITransformGoal goal;

    private ThreadKillerJob threadKiller;


    public TransformJob(IContextService contextService, ITransformService<P, A, T> transformService,
        IParseResultRequester<P> parseResultProcessor, IAnalysisResultRequester<P, A> analysisResultProcessor,
        ILanguageImpl language, Iterable<TransformResource> resources, ITransformGoal goal) {
        super("Transforming resources");

        this.contextService = contextService;
        this.transformService = transformService;

        this.parseResultRequester = parseResultProcessor;
        this.analysisResultRequester = analysisResultProcessor;

        this.language = language;
        this.resources = resources;

        this.goal = goal;
    }


    @Override protected IStatus run(IProgressMonitor monitor) {
        try {
            return transformAll(monitor);
        } catch(ThreadDeath e) {
            return StatusUtils.cancel();
        } finally {
            if(threadKiller != null) {
                threadKiller.cancel();
            }
            monitor.done();
        }
    }

    @Override protected void canceling() {
        final Thread thread = getThread();
        if(thread == null) {
            return;
        }

        logger.debug("Cancelling transform job for {}, interrupting in {}ms, killing in {}ms",
            Joiner.on(", ").join(resources), interruptTimeMillis, interruptTimeMillis + killTimeMillis);
        threadKiller = new ThreadKillerJob(thread, killTimeMillis);
        threadKiller.schedule(interruptTimeMillis);
    }

    private IStatus transformAll(IProgressMonitor progressMonitor) {
        final SubMonitor monitor = SubMonitor.convert(progressMonitor);

        if(monitor.isCanceled())
            return StatusUtils.cancel();

        final SubMonitor loopMonitor = monitor.newChild(1).setWorkRemaining(Iterables.size(resources));
        for(TransformResource transformResource : resources) {
            if(loopMonitor.isCanceled())
                return StatusUtils.cancel();

            final FileObject resource = transformResource.resource;
            loopMonitor.setTaskName("Transforming " + resource);
            try {
                transform(resource, transformResource.project, language, transformResource.text, loopMonitor.newChild(1));
            } catch(ContextException | TransformException e) {
                final String message = logger.format("Transformation failed for {}", resource);
                logger.error(message, e);
                return StatusUtils.error(message, e);
            }
        }

        return StatusUtils.success();
    }

    private void transform(FileObject resource, ILanguageSpec project, ILanguageImpl language, String text, SubMonitor monitor)
        throws ContextException, TransformException {
        final IContext context = contextService.get(resource, project, language);
        if(transformService.requiresAnalysis(context, goal)) {
            monitor.setWorkRemaining(3);
            monitor.setTaskName("Waiting for analysis result");
            final AnalysisFileResult<P, A> result =
                analysisResultRequester.request(resource, context, text).toBlocking().single();
            monitor.worked(1);
            monitor.setTaskName("Waiting for context read lock");
            try(IClosableLock lock = context.read()) {
                monitor.worked(1);
                monitor.setTaskName("Transforming " + resource);
                transformService.transform(result, context, goal);
                monitor.worked(1);
            }
        } else {
            monitor.setWorkRemaining(2);
            monitor.setTaskName("Waiting for parse result");
            final ParseResult<P> result = parseResultRequester.request(resource, language, text).toBlocking().single();
            monitor.worked(1);
            monitor.setTaskName("Transforming " + resource);
            transformService.transform(result, context, goal);
            monitor.worked(1);
        }
    }
}
