package org.metaborg.spoofax.eclipse.editor;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.IEditorInput;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.MetaborgRuntimeException;
import org.metaborg.core.analysis.AnalysisException;
import org.metaborg.core.analysis.AnalysisFileResult;
import org.metaborg.core.analysis.AnalysisMessageResult;
import org.metaborg.core.analysis.AnalysisResult;
import org.metaborg.core.analysis.IAnalysisService;
import org.metaborg.core.context.IContext;
import org.metaborg.core.context.IContextService;
import org.metaborg.core.language.ILanguageIdentifierService;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.language.dialect.IDialectService;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.messages.MessageFactory;
import org.metaborg.core.messages.MessageType;
import org.metaborg.core.outline.IOutline;
import org.metaborg.core.outline.IOutlineService;
import org.metaborg.core.processing.analyze.IAnalysisResultUpdater;
import org.metaborg.core.processing.parse.IParseResultUpdater;
import org.metaborg.core.style.ICategorizerService;
import org.metaborg.core.style.IRegionCategory;
import org.metaborg.core.style.IRegionStyle;
import org.metaborg.core.style.IStylerService;
import org.metaborg.core.syntax.ISyntaxService;
import org.metaborg.core.syntax.ParseException;
import org.metaborg.core.syntax.ParseResult;
import org.metaborg.spoofax.core.style.CategorizerValidator;
import org.metaborg.spoofax.eclipse.job.ThreadKillerJob;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.eclipse.util.MarkerUtils;
import org.metaborg.spoofax.eclipse.util.Nullable;
import org.metaborg.spoofax.eclipse.util.StatusUtils;
import org.metaborg.util.concurrent.IClosableLock;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

public class EditorUpdateJob<P, A> extends Job {
    private static final ILogger logger = LoggerUtils.logger(EditorUpdateJob.class);
    private static final long interruptTimeMillis = 5000;
    private static final long killTimeMillis = 10000;

    private final IEclipseResourceService resourceService;
    private final ILanguageIdentifierService languageIdentifierService;
    private final IDialectService dialectService;
    private final IContextService contextService;
    private final ISyntaxService<P> syntaxService;
    private final IAnalysisService<P, A> analyzer;
    private final ICategorizerService<P, A> categorizer;
    private final IStylerService<P, A> styler;
    private final IOutlineService<P, A> outlineService;
    private final IParseResultUpdater<P> parseResultProcessor;
    private final IAnalysisResultUpdater<P, A> analysisResultProcessor;

    private final IEclipseEditor<P> editor;
    private final IEditorInput input;
    private final @Nullable IResource eclipseResource;
    private final FileObject resource;
    private final String text;
    private final boolean instantaneous;

    private ThreadKillerJob threadKiller;


    public EditorUpdateJob(IEclipseResourceService resourceService,
        ILanguageIdentifierService languageIdentifierService, IDialectService dialectService,
        IContextService contextService, ISyntaxService<P> syntaxService, IAnalysisService<P, A> analyzer,
        ICategorizerService<P, A> categorizer, IStylerService<P, A> styler, IOutlineService<P, A> outlineService,
        IParseResultUpdater<P> parseResultProcessor, IAnalysisResultUpdater<P, A> analysisResultProcessor,
        IEclipseEditor<P> editor, IEditorInput input, @Nullable IResource eclipseResource, FileObject resource,
        String text, boolean instantaneous) {
        super("Updating Spoofax editor");
        setPriority(Job.SHORT);

        this.resourceService = resourceService;
        this.languageIdentifierService = languageIdentifierService;
        this.dialectService = dialectService;
        this.contextService = contextService;
        this.syntaxService = syntaxService;
        this.analyzer = analyzer;
        this.categorizer = categorizer;
        this.styler = styler;
        this.outlineService = outlineService;
        this.parseResultProcessor = parseResultProcessor;
        this.analysisResultProcessor = analysisResultProcessor;

        this.editor = editor;
        this.input = input;
        this.eclipseResource = eclipseResource;
        this.resource = resource;
        this.text = text;
        this.instantaneous = instantaneous;
    }


    @Override public boolean belongsTo(Object family) {
        return input.equals(family);
    }

    @Override protected IStatus run(final IProgressMonitor monitor) {
        logger.debug("Running editor update job for {}", resource);

        final IWorkspace workspace = ResourcesPlugin.getWorkspace();

        try {
            final IStatus status = update(workspace, monitor);
            if(threadKiller != null) {
                threadKiller.cancel();
            }
            return status;
        } catch(MetaborgRuntimeException | MetaborgException | CoreException e) {
            if(threadKiller != null) {
                threadKiller.cancel();
            }
            if(monitor.isCanceled()) {
                return StatusUtils.cancel();
            }

            if(eclipseResource != null) {
                try {
                    final IWorkspaceRunnable parseMarkerUpdater = new IWorkspaceRunnable() {
                        @Override public void run(IProgressMonitor workspaceMonitor) throws CoreException {
                            if(workspaceMonitor.isCanceled())
                                return;
                            MarkerUtils.clearAll(eclipseResource);
                            MarkerUtils.createMarker(eclipseResource, MessageFactory.newErrorAtTop(resource,
                                "Failed to update editor; see the console or error log for more information", MessageType.INTERNAL, e));
                        }
                    };
                    workspace.run(parseMarkerUpdater, eclipseResource, IWorkspace.AVOID_UPDATE, monitor);
                } catch(CoreException e2) {
                    final String message = logger.format("Failed to show internal error marker for {}", resource);
                    logger.error(message, e2);
                    return StatusUtils.silentError(message, e2);
                }
            }

            final String message = logger.format("Failed to update editor for {}", resource);
            logger.error(message, e);
            return StatusUtils.silentError(message, e);
        } catch(Throwable e) {
            return StatusUtils.cancel();
        } finally {
            monitor.done();
        }
    }

    @Override protected void canceling() {
        final Thread thread = getThread();
        if(thread == null) {
            return;
        }

        logger.debug("Cancelling editor update job for {}, interrupting in {}ms, killing in {}ms", resource,
            interruptTimeMillis, interruptTimeMillis + killTimeMillis);
        threadKiller = new ThreadKillerJob(thread, killTimeMillis);
        threadKiller.schedule(interruptTimeMillis);
    }


    private IStatus update(IWorkspace workspace, final IProgressMonitor progressMonitor) throws MetaborgException,
        CoreException {
        final SubMonitor monitor = SubMonitor.convert(progressMonitor, 11);

        monitor.subTask("Identifying language");
        final ILanguageImpl parserLanguage = languageIdentifierService.identify(resource);
        if(parserLanguage == null) {
            throw new MetaborgException("Language could not be identified");
        }
        ILanguageImpl baseLanguage = dialectService.getBase(parserLanguage);
        final ILanguageImpl language;
        if(baseLanguage == null) {
            language = parserLanguage;
        } else {
            language = baseLanguage;
        }
        monitor.worked(1);

        if(monitor.isCanceled())
            return StatusUtils.cancel();
        monitor.subTask("Parsing");
        final ParseResult<P> parseResult = parse(parserLanguage);
        monitor.worked(1);

        if(parseResult.result != null) {
            if(monitor.isCanceled())
                return StatusUtils.cancel();
            monitor.subTask("Styling");
            style(monitor, language, parseResult);
            monitor.worked(1);

            if(monitor.isCanceled())
                return StatusUtils.cancel();
            monitor.subTask("Creating outline");
            outline(monitor, language, parseResult);
            monitor.worked(1);
        } else {
            monitor.worked(2);
        }

        // Just parse when eclipse resource is null, skip the rest. Analysis only works with a project context,
        // which is unavailable when the eclipse resource is null.
        if(eclipseResource == null) {
            return StatusUtils.success();
        }

        // Sleep before showing parse messages to prevent showing irrelevant messages while user is still typing.
        if(!instantaneous && eclipseResource != null) {
            try {
                monitor.subTask("Waiting");
                Thread.sleep(300);
            } catch(InterruptedException e) {
                return StatusUtils.cancel();
            }
        }
        monitor.worked(1);

        if(monitor.isCanceled())
            return StatusUtils.cancel();
        monitor.subTask("Processing parse messages");
        parseMessages(workspace, monitor.newChild(1), parseResult);
        monitor.worked(1);

        // Stop if parsing produced no AST
        if(parseResult.result == null) {
            return StatusUtils.silentError();
        }

        // Sleep before analyzing to prevent running many analyses when small edits are made in succession.
        if(!instantaneous) {
            try {
                monitor.subTask("Waiting");
                Thread.sleep(300);
            } catch(InterruptedException e) {
                return StatusUtils.cancel();
            }
        }
        monitor.worked(1);

        if(monitor.isCanceled())
            return StatusUtils.cancel();
        if(!contextService.available(language))
            return StatusUtils.success();
        monitor.subTask("Analyzing");
        final IContext context = contextService.get(resource, language);
        final AnalysisResult<P, A> analysisResult = analyze(parseResult, context);
        monitor.worked(1);

        if(monitor.isCanceled())
            return StatusUtils.cancel();
        monitor.subTask("Processing analysis messages");
        analysisMessages(workspace, monitor.newChild(1), analysisResult);
        monitor.worked(1);

        return StatusUtils.success();
    }


    private ParseResult<P> parse(ILanguageImpl parserLanguage) throws ParseException, ThreadDeath {
        final ParseResult<P> parseResult;
        try {
            parseResultProcessor.invalidate(resource);
            parseResult = syntaxService.parse(text, resource, parserLanguage, null);
            parseResultProcessor.update(resource, parseResult);
        } catch(ParseException e) {
            parseResultProcessor.error(resource, e);
            throw e;
        } catch(ThreadDeath e) {
            parseResultProcessor.error(resource, new ParseException(resource, parserLanguage,
                "Editor update job killed", e));
            throw e;
        }
        return parseResult;
    }

    private void style(final IProgressMonitor monitor, ILanguageImpl language, ParseResult<P> parseResult) {
        final Iterable<IRegionCategory<P>> categories =
            CategorizerValidator.validate(categorizer.categorize(language, parseResult));
        final Iterable<IRegionStyle<P>> styles = styler.styleParsed(language, categories);
        editor.setStyle(styles, text, monitor);
    }

    private void outline(final IProgressMonitor monitor, ILanguageImpl language, ParseResult<P> parseResult)
        throws MetaborgException {
        if(!outlineService.available(language)) {
            return;
        }

        final IOutline outline = outlineService.outline(parseResult);
        if(outline == null) {
            return;
        }

        editor.setOutline(outline, monitor);
    }

    private void parseMessages(IWorkspace workspace, IProgressMonitor monitor, final ParseResult<P> parseResult)
        throws CoreException {
        // Update markers atomically using a workspace runnable, to prevent flashing/jumping markers.
        final IWorkspaceRunnable parseMarkerUpdater = new IWorkspaceRunnable() {
            @Override public void run(IProgressMonitor workspaceMonitor) throws CoreException {
                if(workspaceMonitor.isCanceled())
                    return;
                MarkerUtils.clearInternal(eclipseResource);
                MarkerUtils.clearParser(eclipseResource);
                for(IMessage message : parseResult.messages) {
                    MarkerUtils.createMarker(eclipseResource, message);
                }
            }
        };
        workspace.run(parseMarkerUpdater, eclipseResource, IWorkspace.AVOID_UPDATE, monitor);
    }

    private AnalysisResult<P, A> analyze(ParseResult<P> parseResult, IContext context) throws AnalysisException,
        ThreadDeath {
        final AnalysisResult<P, A> analysisResult;
        try(IClosableLock lock = context.write()) {
            analysisResultProcessor.invalidate(parseResult.source);
            try {
                analysisResult = analyzer.analyze(Iterables2.singleton(parseResult), context);
            } catch(AnalysisException e) {
                analysisResultProcessor.error(resource, e);
                throw e;
            } catch(ThreadDeath e) {
                analysisResultProcessor.error(resource, new AnalysisException(context, "Editor update job killed", e));
                throw e;
            }
            analysisResultProcessor.update(analysisResult);
        }
        return analysisResult;
    }

    private void analysisMessages(IWorkspace workspace, IProgressMonitor monitor,
        final AnalysisResult<P, A> analysisResult) throws CoreException {
        // Update markers atomically using a workspace runnable, to prevent flashing/jumping markers.
        final IWorkspaceRunnable analysisMarkerUpdater = new IWorkspaceRunnable() {
            @Override public void run(IProgressMonitor workspaceMonitor) throws CoreException {
                if(workspaceMonitor.isCanceled())
                    return;
                MarkerUtils.clearInternal(eclipseResource);
                MarkerUtils.clearAnalysis(eclipseResource);
                for(AnalysisFileResult<P, A> result : analysisResult.fileResults) {
                    if(workspaceMonitor.isCanceled())
                        return;
                    for(IMessage message : result.messages) {
                        if(workspaceMonitor.isCanceled())
                            return;
                        MarkerUtils.createMarker(eclipseResource, message);
                    }
                }

                for(AnalysisMessageResult result : analysisResult.messageResults) {
                    if(workspaceMonitor.isCanceled())
                        return;
                    final IResource messagesEclipseResource = resourceService.unresolve(result.source);
                    MarkerUtils.clearAnalysis(messagesEclipseResource);
                    for(IMessage message : result.messages) {
                        if(workspaceMonitor.isCanceled())
                            return;
                        MarkerUtils.createMarker(messagesEclipseResource, message);
                    }
                }
            }
        };
        workspace.run(analysisMarkerUpdater, eclipseResource, IWorkspace.AVOID_UPDATE, monitor);
    }
}
