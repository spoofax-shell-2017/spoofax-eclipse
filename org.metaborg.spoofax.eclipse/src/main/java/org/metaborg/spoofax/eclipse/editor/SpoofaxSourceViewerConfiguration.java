package org.metaborg.spoofax.eclipse.editor;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.jface.text.DefaultTextHover;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.URLHyperlinkDetector;
import org.eclipse.jface.text.source.DefaultAnnotationHover;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.metaborg.spoofax.core.completion.ICompletionService;
import org.metaborg.spoofax.core.language.ILanguage;
import org.metaborg.spoofax.core.processing.analyze.IAnalysisResultRequester;
import org.metaborg.spoofax.core.processing.parse.IParseResultRequester;
import org.metaborg.spoofax.core.syntax.ISyntaxService;
import org.metaborg.spoofax.core.tracing.IReferenceResolver;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;

public class SpoofaxSourceViewerConfiguration<P, A> extends SourceViewerConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(SourceViewerConfiguration.class);

    private final IEclipseResourceService resourceService;
    private final ISyntaxService<P> syntaxService;
    private final IParseResultRequester<P> parseResultRequester;
    private final IAnalysisResultRequester<P, A> analysisResultRequester;
    private final IReferenceResolver<P, A> referenceResolver;
    private final ICompletionService completionService;

    private final SpoofaxEditor editor;


    public SpoofaxSourceViewerConfiguration(IEclipseResourceService resourceService, ISyntaxService<P> syntaxService,
        IParseResultRequester<P> parseResultRequester, IAnalysisResultRequester<P, A> analysisResultRequester,
        IReferenceResolver<P, A> referenceResolver, ICompletionService completionService, SpoofaxEditor editor) {
        super();

        this.resourceService = resourceService;
        this.syntaxService = syntaxService;
        this.parseResultRequester = parseResultRequester;
        this.analysisResultRequester = analysisResultRequester;
        this.referenceResolver = referenceResolver;
        this.completionService = completionService;

        this.editor = editor;
    }

    @Override public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
        return new DefaultAnnotationHover();
    }

    @Override public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
        return new DefaultTextHover(sourceViewer);
    }

    @Override public String[] getDefaultPrefixes(ISourceViewer sourceViewer, String contentType) {
        final ILanguage language = editor.language();
        if(language == null) {
            logger.warn("Identified language for {} is null, toggle comment is disabled until language is identified",
                editor.resource());
            return new String[0];
        }
        return Iterables.toArray(syntaxService.singleLineCommentPrefixes(language), String.class);
    }

    @Override public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
        final FileObject resource = editor.resource();
        final ILanguage language = editor.language();
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
        final ILanguage language = editor.language();

        if(language == null) {
            logger.warn(
                "Identified language for {} is null, reference resolution is disabled until language is identified",
                resource);
            return new IHyperlinkDetector[] { new URLHyperlinkDetector() };
        }

        return new IHyperlinkDetector[] {
            new SpoofaxHyperlinkDetector<P, A>(resourceService, analysisResultRequester, referenceResolver, resource,
                editor), new URLHyperlinkDetector() };
    }
}
