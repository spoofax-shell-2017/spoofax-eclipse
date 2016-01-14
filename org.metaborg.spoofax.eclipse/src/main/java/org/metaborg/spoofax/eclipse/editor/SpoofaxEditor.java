package org.metaborg.spoofax.eclipse.editor;

import java.awt.Color;
import java.util.Collection;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.ITextViewerExtension4;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.source.DefaultCharacterPairMatcher;
import org.eclipse.jface.text.source.ICharacterPairMatcher;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.ISourceViewerExtension2;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.metaborg.core.analysis.IAnalysisService;
import org.metaborg.core.completion.ICompletionService;
import org.metaborg.core.context.IContextService;
import org.metaborg.core.language.ILanguageIdentifierService;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.language.dialect.IDialectService;
import org.metaborg.core.outline.IOutline;
import org.metaborg.core.outline.IOutlineService;
import org.metaborg.core.style.ICategorizerService;
import org.metaborg.core.style.IRegionStyle;
import org.metaborg.core.style.IStylerService;
import org.metaborg.core.syntax.FenceCharacters;
import org.metaborg.core.syntax.ISyntaxService;
import org.metaborg.spoofax.core.processing.analyze.ISpoofaxAnalysisResultProcessor;
import org.metaborg.spoofax.core.processing.parse.ISpoofaxParseResultProcessor;
import org.metaborg.spoofax.core.tracing.ISpoofaxHoverService;
import org.metaborg.spoofax.core.tracing.ISpoofaxResolverService;
import org.metaborg.spoofax.eclipse.SpoofaxPlugin;
import org.metaborg.spoofax.eclipse.editor.outline.SpoofaxOutlinePage;
import org.metaborg.spoofax.eclipse.editor.outline.SpoofaxOutlinePopup;
import org.metaborg.spoofax.eclipse.job.GlobalSchedulingRules;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.eclipse.util.Nullable;
import org.metaborg.spoofax.eclipse.util.StyleUtils;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.terms.IStrategoTerm;

import com.google.common.collect.Lists;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

public class SpoofaxEditor extends TextEditor implements IEclipseEditor<IStrategoTerm> {
    public static final String id = SpoofaxPlugin.id + ".editor";

    private static final ILogger logger = LoggerUtils.logger(SpoofaxEditor.class);

    private IEclipseResourceService resourceService;
    private ILanguageIdentifierService languageIdentifier;
    private IDialectService dialectService;
    private IContextService contextService;
    private ISyntaxService<IStrategoTerm> syntaxService;
    private IAnalysisService<IStrategoTerm, IStrategoTerm> analysisService;
    private ICategorizerService<IStrategoTerm, IStrategoTerm> categorizerService;
    private IStylerService<IStrategoTerm, IStrategoTerm> stylerService;
    private IOutlineService<IStrategoTerm, IStrategoTerm> outlineService;
    private ICompletionService completionService;
    private ISpoofaxResolverService referenceResolver;
    private ISpoofaxHoverService hoverService;
    private ISpoofaxParseResultProcessor parseResultProcessor;
    private ISpoofaxAnalysisResultProcessor analysisResultProcessor;

    private GlobalSchedulingRules globalRules;

    private IJobManager jobManager;

    private final IPropertyListener editorInputChangedListener;
    private final PresentationMerger presentationMerger;
    private final SpoofaxOutlinePage outlinePage;
    private SpoofaxOutlinePopup outlinePopup;

    private DocumentListener documentListener;
    private ISourceViewer sourceViewer;
    private ISourceViewerExtension2 sourceViewerExt2;
    private ITextViewerExtension4 textViewerExt4;

    private IEditorInput input;
    private String inputName;
    private @Nullable IResource eclipseResource;
    private IDocument document;
    private @Nullable FileObject resource;
    private ILanguageImpl language;


    public SpoofaxEditor() {
        super(); // Super constructor will call into initializeEditor, initialize fields in that method.

        this.editorInputChangedListener = new EditorInputChangedListener();
        this.presentationMerger = new PresentationMerger();
        this.outlinePage = new SpoofaxOutlinePage(this);
    }


    @Override public @Nullable FileObject resource() {
        if(!checkInitialized()) {
            return null;
        }
        return resource;
    }

    @Override public @Nullable ILanguageImpl language() {
        if(!checkInitialized()) {
            return null;
        }
        return language;
    }

    @Override public boolean enabled() {
        return documentListener != null;
    }

    @Override public void enable() {
        if(!checkInitialized() || enabled()) {
            return;
        }
        logger.debug("Enabling editor for {}", inputName);
        documentListener = new DocumentListener();
        document.addDocumentListener(documentListener);
        scheduleJob(true);
    }

    @Override public void disable() {
        if(!checkInitialized() || !enabled()) {
            return;
        }
        logger.debug("Disabling editor for {}", inputName);
        document.removeDocumentListener(documentListener);
        documentListener = null;

        final Display display = Display.getDefault();
        final TextPresentation blackPresentation =
            StyleUtils.createTextPresentation(Color.BLACK, document.getLength(), display);
        presentationMerger.invalidate();
        display.asyncExec(new Runnable() {
            @Override public void run() {
                sourceViewer.changeTextPresentation(blackPresentation, true);
            }
        });
    }

    @Override public void forceUpdate() {
        if(!checkInitialized()) {
            return;
        }
        logger.debug("Force updating editor for {}", inputName);
        scheduleJob(true);
    }

    @Override public void reconfigure() {
        if(!checkInitialized()) {
            return;
        }
        logger.debug("Reconfiguring editor for {}", inputName);
        // Don't identify language if plugin is still loading, to prevent deadlocks.
        if(resource != null && SpoofaxPlugin.doneLoading()) {
            language = languageIdentifier.identify(resource);
        } else {
            language = null;
        }

        final Display display = Display.getDefault();
        display.asyncExec(new Runnable() {
            @Override public void run() {
                sourceViewerExt2.unconfigure();
                setSourceViewerConfiguration(createSourceViewerConfiguration());
                sourceViewer.configure(getSourceViewerConfiguration());
                final SourceViewerDecorationSupport decorationSupport = getSourceViewerDecorationSupport(sourceViewer);
                configureSourceViewerDecorationSupport(decorationSupport);
                decorationSupport.uninstall();
                decorationSupport.install(getPreferenceStore());
            }
        });
    }


    @Override public @Nullable IEditorInput input() {
        if(!checkInitialized()) {
            return null;
        }
        return input;
    }

    @Override public @Nullable IResource eclipseResource() {
        if(!checkInitialized()) {
            return null;
        }
        return eclipseResource;
    }

    @Override public @Nullable IDocument document() {
        if(!checkInitialized()) {
            return null;
        }
        return document;
    }

    @Override public @Nullable ISourceViewer sourceViewer() {
        if(!checkInitialized()) {
            return null;
        }
        return getSourceViewer();
    }

    @Override public @Nullable SourceViewerConfiguration configuration() {
        if(!checkInitialized()) {
            return null;
        }
        return getSourceViewerConfiguration();
    }


    @Override public void setStyle(Iterable<IRegionStyle<IStrategoTerm>> style, final String text,
        final IProgressMonitor monitor) {
        final Display display = Display.getDefault();

        final TextPresentation textPresentation = StyleUtils.createTextPresentation(style, display);
        presentationMerger.set(textPresentation);

        // Update styling on the main thread, required by Eclipse.
        display.asyncExec(new Runnable() {
            public void run() {
                if(monitor.isCanceled())
                    return;
                // Also cancel if text presentation is not valid for current text any more.
                if(document == null || !document.get().equals(text)) {
                    return;
                }
                sourceViewer.changeTextPresentation(textPresentation, true);
            }
        });
    }

    @Override public void setOutline(final IOutline outline, final IProgressMonitor monitor) {
        final Display display = Display.getDefault();

        // Update outline on the main thread, required by Eclipse.
        display.asyncExec(new Runnable() {
            public void run() {
                if(monitor.isCanceled())
                    return;
                outlinePage.update(outline);
                outlinePopup.update(outline);
            }
        });
    }


    @Override public void openQuickOutline() {
        outlinePopup.open();
    }


    @Override protected void initializeEditor() {
        super.initializeEditor();

        SpoofaxEditorPreferences.setDefaults(getPreferenceStore());

        // Initialize fields here instead of constructor, so that we can pass fields to the source viewer configuration.
        final Injector injector = SpoofaxPlugin.injector();
        this.resourceService = injector.getInstance(IEclipseResourceService.class);
        this.languageIdentifier = injector.getInstance(ILanguageIdentifierService.class);
        this.dialectService = injector.getInstance(IDialectService.class);
        this.contextService = injector.getInstance(IContextService.class);
        this.syntaxService = injector.getInstance(Key.get(new TypeLiteral<ISyntaxService<IStrategoTerm>>() {}));
        this.analysisService =
            injector.getInstance(Key.get(new TypeLiteral<IAnalysisService<IStrategoTerm, IStrategoTerm>>() {}));
        this.categorizerService =
            injector.getInstance(Key.get(new TypeLiteral<ICategorizerService<IStrategoTerm, IStrategoTerm>>() {}));
        this.stylerService =
            injector.getInstance(Key.get(new TypeLiteral<IStylerService<IStrategoTerm, IStrategoTerm>>() {}));
        this.outlineService =
            injector.getInstance(Key.get(new TypeLiteral<IOutlineService<IStrategoTerm, IStrategoTerm>>() {}));
        this.completionService = injector.getInstance(ICompletionService.class);
        this.referenceResolver = injector.getInstance(ISpoofaxResolverService.class);
        this.hoverService = injector.getInstance(ISpoofaxHoverService.class);
        this.parseResultProcessor = injector.getInstance(ISpoofaxParseResultProcessor.class);
        this.analysisResultProcessor = injector.getInstance(ISpoofaxAnalysisResultProcessor.class);
        this.globalRules = injector.getInstance(GlobalSchedulingRules.class);
        this.jobManager = Job.getJobManager();

        setDocumentProvider(new SpoofaxDocumentProvider(resourceService));
        setEditorContextMenuId("#SpoofaxEditorContext");
        setSourceViewerConfiguration(createSourceViewerConfiguration());
    }

    private SourceViewerConfiguration createSourceViewerConfiguration() {
        return new SpoofaxSourceViewerConfiguration<IStrategoTerm, IStrategoTerm>(resourceService, syntaxService,
            parseResultProcessor, analysisResultProcessor, referenceResolver, hoverService, completionService,
            getPreferenceStore(), this);
    }

    @Override protected ISourceViewer createSourceViewer(Composite parent, IVerticalRuler ruler, int styles) {
        // Store current input and document so we have access to them when the editor input changes.
        input = getEditorInput();
        document = getDocumentProvider().getDocument(input);

        // Store resources for future use.
        resource = resourceService.resolve(input);
        if(resource != null) {
            inputName = resource.toString();
            eclipseResource = resourceService.unresolve(resource);

            // Don't identify language if plugin is still loading, to prevent deadlocks.
            if(SpoofaxPlugin.doneLoading()) {
                // Identify the language for future use. Will be null if this editor was opened when Eclipse opened,
                // because languages have not been discovered yet.
                language = languageIdentifier.identify(resource);
            }
        } else {
            inputName = input.getName();
            logger.warn("Resource for editor on {} is null, cannot update the editor", inputName);
        }

        // Create source viewer after input, document, resources, and language have been set.
        sourceViewer = super.createSourceViewer(parent, ruler, styles);
        sourceViewerExt2 = (ISourceViewerExtension2) sourceViewer;
        textViewerExt4 = (ITextViewerExtension4) sourceViewer;

        // Register for changes in the text, to schedule editor updates.
        documentListener = new DocumentListener();
        document.addDocumentListener(documentListener);

        // Register for changes in the editor input, to handle renaming or moving of resources of open editors.
        this.addPropertyListener(editorInputChangedListener);

        // Register for changes in text presentation, to merge our text presentation with presentations from other
        // sources, such as marker annotations.
        textViewerExt4.addTextPresentationListener(presentationMerger);

        // Create quick outline control.
        this.outlinePopup = new SpoofaxOutlinePopup(getSite().getShell(), this);

        scheduleJob(true);

        return sourceViewer;
    }

    @Override protected void configureSourceViewerDecorationSupport(SourceViewerDecorationSupport support) {
        super.configureSourceViewerDecorationSupport(support);

        if(language == null) {
            logger.debug("Cannot get language-specific fences, identified language for {} is null, "
                + "bracket matching is disabled until language is identified", inputName);
            return;
        }

        final Iterable<FenceCharacters> fenceCharacters = syntaxService.fenceCharacters(language);
        final Collection<Character> pairMatcherChars = Lists.newArrayList();
        for(FenceCharacters fenceChars : fenceCharacters) {
            final String open = fenceChars.open;
            final String close = fenceChars.close;
            if(open.length() > 1 || close.length() > 1) {
                logger.debug("Multi-character fences {} {} are not supported in Eclipse", open, close);
                continue;
            }
            pairMatcherChars.add(open.charAt(0));
            pairMatcherChars.add(close.charAt(0));
        }

        final ICharacterPairMatcher matcher =
            new DefaultCharacterPairMatcher(ArrayUtils.toPrimitive(pairMatcherChars.toArray(new Character[0])));
        support.setCharacterPairMatcher(matcher);
        SpoofaxEditorPreferences.setPairMatcherKeys(support);
    }

    @Override public void dispose() {
        cancelJobs(input);

        if(documentListener != null) {
            document.removeDocumentListener(documentListener);
        }
        this.removePropertyListener(editorInputChangedListener);
        if(textViewerExt4 != null) {
            textViewerExt4.removeTextPresentationListener(presentationMerger);
        }

        input = null;
        inputName = null;
        resource = null;
        eclipseResource = null;
        document = null;
        language = null;
        sourceViewer = null;
        textViewerExt4 = null;
        documentListener = null;

        super.dispose();
    }

    @Override public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
        if(IContentOutlinePage.class.equals(adapter)) {
            return outlinePage;
        }
        return super.getAdapter(adapter);
    }


    private boolean checkInitialized() {
        if(input == null || sourceViewer == null) {
            logger.error("Attempted to use editor before it was initialized");
            return false;
        }
        return true;
    }

    private void scheduleJob(boolean instantaneous) {
        if(!checkInitialized() || resource == null) {
            return;
        }

        cancelJobs(input);

        // THREADING: invalidate text styling here on the main thread (instead of in the editor update job), to prevent
        // race conditions.
        presentationMerger.invalidate();
        parseResultProcessor.invalidate(resource);
        analysisResultProcessor.invalidate(resource);

        final Job job =
            new EditorUpdateJob<>(resourceService, languageIdentifier, dialectService, contextService, syntaxService,
                analysisService, categorizerService, stylerService, outlineService, parseResultProcessor,
                analysisResultProcessor, this, input, eclipseResource, resource, document.get(), instantaneous);
        final ISchedulingRule rule;
        if(eclipseResource == null) {
            rule = new MultiRule(new ISchedulingRule[] { globalRules.startupReadLock(), globalRules.strategoLock() });
        } else {
            rule =
                new MultiRule(new ISchedulingRule[] { globalRules.startupReadLock(), globalRules.strategoLock(),
                    eclipseResource });
        }
        job.setRule(rule);
        job.schedule(instantaneous ? 0 : 100);
    }

    private void cancelJobs(IEditorInput specificInput) {
        logger.trace("Cancelling editor update jobs for {}", specificInput);
        final Job[] existingJobs = jobManager.find(specificInput);
        for(Job job : existingJobs) {
            job.cancel();
        }
    }

    private void editorInputChanged() {
        final IEditorInput oldInput = input;
        final IDocument oldDocument = document;

        logger.debug("Editor input changed from {} to {}", oldInput, input);

        // Unregister old document listener and register a new one, because the input changed which also changes the
        // document.
        if(documentListener != null) {
            oldDocument.removeDocumentListener(documentListener);
        }
        input = getEditorInput();
        document = getDocumentProvider().getDocument(input);
        documentListener = new DocumentListener();
        document.addDocumentListener(documentListener);

        // Store new resource, because these may have changed as a result of the input change.
        resource = resourceService.resolve(input);
        if(resource != null) {
            inputName = resource.toString();
            eclipseResource = resourceService.unresolve(resource);
        } else {
            inputName = input.getName();
            logger.warn("Resource for editor on {} is null, cannot update the editor", inputName);
        }

        // Reconfigure the editor because the language may have changed.
        reconfigure();

        cancelJobs(oldInput);
        scheduleJob(true);
    }

    private final class DocumentListener implements IDocumentListener {
        @Override public void documentAboutToBeChanged(DocumentEvent event) {

        }

        @Override public void documentChanged(DocumentEvent event) {
            scheduleJob(false);
        }
    }

    private final class EditorInputChangedListener implements IPropertyListener {
        @Override public void propertyChanged(Object source, int propId) {
            if(propId == IEditorPart.PROP_INPUT) {
                editorInputChanged();
            }
        }
    }
}
