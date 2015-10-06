package org.metaborg.spoofax.eclipse.editor;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.jface.internal.text.html.HTMLTextPresenter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.URLHyperlinkDetector;
import org.eclipse.jface.text.quickassist.IQuickAssistAssistant;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.ISourceViewerExtension2;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import org.metaborg.core.completion.ICompletionService;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.processing.analyze.IAnalysisResultRequester;
import org.metaborg.core.processing.parse.IParseResultRequester;
import org.metaborg.core.syntax.ISyntaxService;
import org.metaborg.core.tracing.IHoverService;
import org.metaborg.core.tracing.IResolverService;
import org.metaborg.spoofax.eclipse.editor.completion.SpoofaxContentAssistProcessor;
import org.metaborg.spoofax.eclipse.editor.tracing.SpoofaxHyperlinkDetector;
import org.metaborg.spoofax.eclipse.editor.tracing.SpoofaxTextHover;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;

@SuppressWarnings("restriction")
public class SpoofaxSourceViewerConfiguration<P, A> extends TextSourceViewerConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(SourceViewerConfiguration.class);

    private final IEclipseResourceService resourceService;
    private final ISyntaxService<P> syntaxService;
    private final IParseResultRequester<P> parseResultRequester;
    private final IAnalysisResultRequester<P, A> analysisResultRequester;
    private final IResolverService<P, A> referenceResolver;
    private final IHoverService<P, A> hoverService;
    private final ICompletionService completionService;

    private final SpoofaxEditor editor;


    public SpoofaxSourceViewerConfiguration(IEclipseResourceService resourceService, ISyntaxService<P> syntaxService,
        IParseResultRequester<P> parseResultRequester, IAnalysisResultRequester<P, A> analysisResultRequester,
        IResolverService<P, A> referenceResolver, IHoverService<P, A> hoverService,
        ICompletionService completionService, IPreferenceStore preferenceStore, SpoofaxEditor editor) {
        super(preferenceStore);

        this.resourceService = resourceService;
        this.syntaxService = syntaxService;
        this.parseResultRequester = parseResultRequester;
        this.analysisResultRequester = analysisResultRequester;
        this.referenceResolver = referenceResolver;
        this.hoverService = hoverService;
        this.completionService = completionService;

        this.editor = editor;
    }

    @Override public String[] getDefaultPrefixes(ISourceViewer sourceViewer, String contentType) {
        final ILanguageImpl language = editor.language();
        if(language == null) {
            logger.warn("Identified language for {} is null, toggle comment is disabled until language is identified",
                editor.resource());
            return new String[0];
        }
        return Iterables.toArray(syntaxService.singleLineCommentPrefixes(language), String.class);
    }

    @Override public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
        final FileObject resource = editor.resource();
        final ILanguageImpl language = editor.language();
        final IDocument document = editor.document();
        if(language == null) {
            logger.warn("Identified language for {} is null, content assist is disabled until language is identified",
                resource);
            return null;
        }

        final ContentAssistant assistant = new ContentAssistant();
        final SpoofaxContentAssistProcessor processor =
            new SpoofaxContentAssistProcessor(completionService, parseResultRequester, resource, document, language);
        assistant.setContentAssistProcessor(processor, IDocument.DEFAULT_CONTENT_TYPE);
        assistant.setRepeatedInvocationMode(true);
        return assistant;
    }

    @Override public IHyperlinkDetector[] getHyperlinkDetectors(ISourceViewer sourceViewer) {
        final FileObject resource = editor.resource();
        final ILanguageImpl language = editor.language();

        if(language == null) {
            logger.warn(
                "Identified language for {} is null, reference resolution is disabled until language is identified",
                resource);
            return new IHyperlinkDetector[] { new URLHyperlinkDetector() };
        }

        return new IHyperlinkDetector[] {
            new SpoofaxHyperlinkDetector<P, A>(resourceService, parseResultRequester, analysisResultRequester,
                referenceResolver, resource, language, editor), new URLHyperlinkDetector() };
    }

    @Override public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
        final FileObject resource = editor.resource();
        final ILanguageImpl language = editor.language();

        if(language == null) {
            logger.warn("Identified language for {} is null, hover tooltips are disabled until language is identified",
                resource);
            return null;
        }

        return new SpoofaxTextHover<P, A>(parseResultRequester, analysisResultRequester, hoverService, resource,
            language, (ISourceViewerExtension2) editor.sourceViewer());
    }

    public IInformationControlCreator getInformationControlCreator(ISourceViewer sourceViewer) {
        return new IInformationControlCreator() {
            public IInformationControl createInformationControl(Shell parent) {
                return new DefaultInformationControl(parent, new HTMLTextPresenter(true));
            }
        };
    }

    @Override public IReconciler getReconciler(ISourceViewer sourceViewer) {
        // Return null to disable TextSourceViewerConfiguration reconciler which does spell checking.
        return null;
    }

    @Override public IQuickAssistAssistant getQuickAssistAssistant(ISourceViewer sourceViewer) {
        // Return null to disable TextSourceViewerConfiguration quick assist which does spell checking.
        return null;
    }
}
