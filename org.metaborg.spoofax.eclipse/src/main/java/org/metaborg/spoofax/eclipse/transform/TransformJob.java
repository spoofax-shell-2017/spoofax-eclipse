package org.metaborg.spoofax.eclipse.transform;

import java.io.IOException;
import java.util.List;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
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
import org.metaborg.spoofax.eclipse.util.StatusUtils;
import org.metaborg.util.concurrent.IClosableLock;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

public class TransformJob<P, A, T> extends Job {
    private static final ILogger logger = LoggerUtils.logger(TransformJob.class);

    private final IContextService contextService;
    private final IMenuService menuService;
    private final ITransformer<P, A, T> transformer;

    private final IParseResultRequester<P> parseResultRequester;
    private final IAnalysisResultRequester<P, A> analysisResultRequester;

    private final ILanguageImpl language;
    private final Iterable<TransformResource> resources;
    private final List<String> actionNames;


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
        if(monitor.isCanceled())
            return StatusUtils.cancel();

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
                String.format("Transformation failed, action %s is not a Stratego transformer action", goal);
            logger.error(message);
            return StatusUtils.error(message);
        }

        for(TransformResource transformResource : resources) {
            if(monitor.isCanceled())
                return StatusUtils.cancel();

            final FileObject resource = transformResource.resource;
            try {
                transform(resource, language, (TransformAction) action, goal, transformResource.text);
            } catch(IOException | ContextException | TransformerException e) {
                final String message = String.format("Transformation failed for %s", resource);
                logger.error(message, e);
                return StatusUtils.error(message, e);
            }
        }

        return StatusUtils.success();
    }

    private void transform(FileObject resource, ILanguageImpl language, TransformAction action,
        ITransformerGoal goal, String text) throws IOException, ContextException, TransformerException {
        final IContext context = contextService.get(resource, language);
        if(action.flags.parsed) {
            final ParseResult<P> result = parseResultRequester.request(resource, language, text).toBlocking().single();
            transformer.transform(result, context, goal);
        } else {
            final AnalysisFileResult<P, A> result =
                analysisResultRequester.request(resource, context, text).toBlocking().single();
            try(IClosableLock lock = context.read()) {
                transformer.transform(result, context, goal);
            }
        }
    }
}
