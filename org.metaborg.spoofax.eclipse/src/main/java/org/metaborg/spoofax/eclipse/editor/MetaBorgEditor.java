package org.metaborg.spoofax.eclipse.editor;

import java.awt.Color;
import java.util.Collection;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewerExtension4;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.source.DefaultCharacterPairMatcher;
import org.eclipse.jface.text.source.ICharacterPairMatcher;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.ISourceViewerExtension2;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.metaborg.core.analysis.IAnalysisService;
import org.metaborg.core.analysis.IAnalyzeUnit;
import org.metaborg.core.analysis.IAnalyzeUnitUpdate;
import org.metaborg.core.context.IContextService;
import org.metaborg.core.language.ILanguageIdentifierService;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.outline.IOutline;
import org.metaborg.core.outline.IOutlineService;
import org.metaborg.core.processing.analyze.IAnalysisResultProcessor;
import org.metaborg.core.processing.parse.IParseResultProcessor;
import org.metaborg.core.project.IProjectService;
import org.metaborg.core.style.ICategorizerService;
import org.metaborg.core.style.IRegionStyle;
import org.metaborg.core.style.IStylerService;
import org.metaborg.core.syntax.FenceCharacters;
import org.metaborg.core.syntax.IInputUnit;
import org.metaborg.core.syntax.IParseUnit;
import org.metaborg.core.syntax.ISyntaxService;
import org.metaborg.core.tracing.IHoverService;
import org.metaborg.core.tracing.IResolverService;
import org.metaborg.core.unit.IInputUnitService;
import org.metaborg.spoofax.eclipse.SpoofaxPlugin;
import org.metaborg.spoofax.eclipse.SpoofaxPreferences;
import org.metaborg.spoofax.eclipse.editor.outline.SpoofaxOutlinePage;
import org.metaborg.spoofax.eclipse.editor.outline.SpoofaxOutlinePopup;
import org.metaborg.spoofax.eclipse.job.GlobalSchedulingRules;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.eclipse.util.Nullable;
import org.metaborg.spoofax.eclipse.util.StyleUtils;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.Lists;
import com.google.inject.Injector;

public abstract class MetaBorgEditor<I extends IInputUnit, P extends IParseUnit, A extends IAnalyzeUnit, AU extends IAnalyzeUnitUpdate, F>
    extends TextEditor implements IEclipseEditor<F> {
    private static final ILogger logger = LoggerUtils.logger(MetaBorgEditor.class);

    protected IEclipseResourceService resourceService;
    protected ILanguageIdentifierService languageIdentifier;
    protected IProjectService projectService;
    protected IContextService contextService;
    protected IInputUnitService<I> unitService;
    protected ISyntaxService<I, P> syntaxService;
    protected IAnalysisService<P, A, AU> analysisService;
    protected ICategorizerService<P, A, F> categorizerService;
    protected IStylerService<F> stylerService;
    protected IOutlineService<P, A> outlineService;
    protected IResolverService<P, A> resolverService;
    protected IHoverService<P, A> hoverService;
    protected IParseResultProcessor<I, P> parseResultProcessor;
    protected IAnalysisResultProcessor<I, P, A> analysisResultProcessor;

    protected GlobalSchedulingRules globalRules;
    protected SpoofaxPreferences preferences;

    protected IJobManager jobManager;

    protected final IPropertyListener editorInputChangedListener;
    protected final PresentationMerger presentationMerger;
    protected final SpoofaxOutlinePage outlinePage;
    protected SpoofaxOutlinePopup outlinePopup;

    protected DocumentListener documentListener;
    protected ISourceViewer sourceViewer;
    protected ISourceViewerExtension2 sourceViewerExt2;
    protected ITextViewerExtension4 textViewerExt4;

    protected IEditorInput input;
    protected String inputName;
    protected @Nullable IResource eclipseResource;
    protected IDocument document;
    protected @Nullable FileObject resource;
    protected ILanguageImpl language;


    public MetaBorgEditor() {
        /*
         * THREADING: Super constructor will call into initializeEditor, which injects the required services. Injecting
         * services in this constructor here may cause Eclipse to deadlock because of initialization order issues.
         */
        super();

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


    @Override public boolean editorIsUpdating() {
        final Job[] existingJobs = jobManager.find(this);
        return existingJobs.length > 0;
    }


    @Override public ISelectionProvider selectionProvider() {
        return getSelectionProvider();
    }

    @Override public ITextOperationTarget textOperationTarget() {
        return (ITextOperationTarget) getAdapter(ITextOperationTarget.class);
    }


    @Override public void setStyle(Iterable<IRegionStyle<F>> style, final String text, final IProgressMonitor monitor) {
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

        EditorPreferences.setDefaults(getPreferenceStore());

        // Initialize fields here instead of constructor, so that we can pass fields to the source viewer configuration.
        final Injector injector = SpoofaxPlugin.injector();
        injectServices(injector);
        injectGenericServices(injector);
        this.jobManager = Job.getJobManager();

        setDocumentProvider(new DocumentProvider(resourceService));
        setEditorContextMenuId("#SpoofaxEditorContext");
        setSourceViewerConfiguration(createSourceViewerConfiguration());
    }

    protected void injectServices(Injector injector) {
        this.resourceService = injector.getInstance(IEclipseResourceService.class);
        this.languageIdentifier = injector.getInstance(ILanguageIdentifierService.class);
        this.contextService = injector.getInstance(IContextService.class);
        this.projectService = injector.getInstance(IProjectService.class);
        this.globalRules = injector.getInstance(GlobalSchedulingRules.class);
        this.preferences = injector.getInstance(SpoofaxPreferences.class);

    }

    protected abstract void injectGenericServices(Injector injectors);

    private SourceViewerConfiguration createSourceViewerConfiguration() {
        return new MetaBorgSourceViewerConfiguration<>(resourceService, unitService, syntaxService,
            parseResultProcessor, analysisResultProcessor, resolverService, hoverService, getPreferenceStore(), this);
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

        final ICharacterPairMatcher matcher = new DefaultCharacterPairMatcher(
            ArrayUtils.toPrimitive(pairMatcherChars.toArray(new Character[pairMatcherChars.size()])));
        support.setCharacterPairMatcher(matcher);
        EditorPreferences.setPairMatcherKeys(support);
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

    /**
     * DO NOT CHANGE THE SIGNATURE OF THIS METHOD! The signature of this method is such that it is compatible with older
     * Eclipse versions.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" }) @Override public Object getAdapter(Class adapter) {
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

        final long analysisDelayMs = preferences.delayEditorAnalysis() ? 5000 : 500;
        final boolean analysis = !preferences.disableEditorAnalysis();
        final Job job = new EditorUpdateJob<>(resourceService, languageIdentifier, contextService, projectService,
            unitService, syntaxService, analysisService, categorizerService, stylerService, outlineService,
            parseResultProcessor, analysisResultProcessor, this, input, eclipseResource, resource, document.get(),
            instantaneous, analysisDelayMs, analysis);
        final ISchedulingRule rule;
        if(eclipseResource == null) {
            rule = new MultiRule(new ISchedulingRule[] { globalRules.startupReadLock() });
        } else if(!analysis) {
            rule = new MultiRule(new ISchedulingRule[] { globalRules.startupReadLock(), eclipseResource });
        } else {
            rule = new MultiRule(new ISchedulingRule[] { globalRules.startupReadLock(), globalRules.strategoLock(),
                eclipseResource.getProject() });
        }
        job.setRule(rule);
        job.schedule(instantaneous ? 0 : 300);
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
