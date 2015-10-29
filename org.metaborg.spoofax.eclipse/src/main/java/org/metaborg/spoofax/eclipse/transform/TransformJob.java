package org.metaborg.spoofax.eclipse.transform;

import java.io.IOException;
import java.util.List;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.analysis.AnalysisFileResult;
import org.metaborg.core.context.ContextException;
import org.metaborg.core.context.IContext;
import org.metaborg.core.context.IContextService;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.menu.IAction;
import org.metaborg.core.menu.IMenuService;
import org.metaborg.core.processing.analyze.IAnalysisResultRequester;
import org.metaborg.core.processing.parse.IParseResultRequester;
import org.metaborg.core.syntax.ParseResult;
import org.metaborg.core.transform.ITransformer;
import org.metaborg.core.transform.ITransformerGoal;
import org.metaborg.core.transform.NestedNamedGoal;
import org.metaborg.core.transform.TransformerException;
import org.metaborg.spoofax.core.menu.TransformAction;
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
    private final IMenuService menuService;
    private final ITransformer<P, A, T> transformer;

    private final IParseResultRequester<P> parseResultRequester;
    private final IAnalysisResultRequester<P, A> analysisResultRequester;

    private final ILanguageImpl language;
    private final Iterable<TransformResource> resources;
    private final List<String> actionNames;

    private ThreadKillerJob threadKiller;


    public TransformJob(IContextService contextService, IMenuService menuService, ITransformer<P, A, T> transformer,
        IParseResultRequester<P> parseResultProcessor, IAnalysisResultRequester<P, A> analysisResultProcessor,
        ILanguageImpl language, Iterable<TransformResource> resources, List<String> actionNames) {
        super("Transforming resources");

        this.contextService = contextService;
        this.menuService = menuService;
        this.transformer = transformer;

        this.parseResultRequester = parseResultProcessor;
        this.analysisResultRequester = analysisResultProcessor;

        this.language = language;
        this.resources = resources;

        this.actionNames = actionNames;
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
        final SubMonitor monitor = SubMonitor.convert(progressMonitor, 10);

        if(monitor.isCanceled())
            return StatusUtils.cancel();

        monitor.setTaskName("Retrieving action");
        final IAction action;
        try {
            action = menuService.nestedAction(language, actionNames);
        } catch(MetaborgException e) {
            final String message = "Transformation failed";
            logger.error(message, e);
            return StatusUtils.error(message, e);
        }

        final ITransformerGoal goal = new NestedNamedGoal(actionNames);

        if(action == null) {
            final String message =
                logger.format("Transformation failed, {} does not have an action named {}", language, goal);
            logger.error(message);
            return StatusUtils.error(message);
        }

        if(!(action instanceof TransformAction)) {
            final String message =
                String.format("Transformation failed, action {} is not a Stratego transformer action", goal);
            logger.error(message);
            return StatusUtils.error(message);
        }
        monitor.worked(1);

        final SubMonitor loopMonitor = monitor.newChild(9).setWorkRemaining(Iterables.size(resources));
        for(TransformResource transformResource : resources) {
            if(loopMonitor.isCanceled())
                return StatusUtils.cancel();

            final FileObject resource = transformResource.resource;
            loopMonitor.setTaskName("Transforming " + resource);
            try {
                transform(resource, language, (TransformAction) action, goal, transformResource.text,
                    loopMonitor.newChild(1));
            } catch(IOException | ContextException | TransformerException e) {
                final String message = logger.format("Transformation failed for {}", resource);
                logger.error(message, e);
                return StatusUtils.error(message, e);
            }
        }

        return StatusUtils.success();
    }

    private void transform(FileObject resource, ILanguageImpl language, TransformAction action, ITransformerGoal goal,
        String text, SubMonitor monitor) throws IOException, ContextException, TransformerException {
        final IContext context = contextService.get(resource, language);
        if(action.flags.parsed) {
            monitor.setWorkRemaining(2);
            monitor.setTaskName("Waiting for parse result");
            final ParseResult<P> result = parseResultRequester.request(resource, language, text).toBlocking().single();
            monitor.worked(1);
            monitor.setTaskName("Transforming " + resource);
            transformer.transform(result, context, goal);
            monitor.worked(1);
        } else {
            monitor.setWorkRemaining(3);
            monitor.setTaskName("Waiting for analysis result");
            final AnalysisFileResult<P, A> result =
                analysisResultRequester.request(resource, context, text).toBlocking().single();
            monitor.worked(1);
            monitor.setTaskName("Waiting for context read lock");
            try(IClosableLock lock = context.read()) {
                monitor.worked(1);
                monitor.setTaskName("Transforming " + resource);
                transformer.transform(result, context, goal);
                monitor.worked(1);
            }
        }
    }
}
