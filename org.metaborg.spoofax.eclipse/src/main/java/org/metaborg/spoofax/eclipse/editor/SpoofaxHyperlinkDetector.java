package org.metaborg.spoofax.eclipse.editor;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.analysis.AnalysisFileResult;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.processing.analyze.IAnalysisResultRequester;
import org.metaborg.core.processing.parse.IParseResultRequester;
import org.metaborg.core.syntax.ParseResult;
import org.metaborg.core.tracing.IReferenceResolver;
import org.metaborg.core.tracing.Resolution;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.eclipse.util.Nullable;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

public class SpoofaxHyperlinkDetector<P, A> extends AbstractHyperlinkDetector {
    private static final ILogger logger = LoggerUtils.logger(SpoofaxHyperlinkDetector.class);

    private final IEclipseResourceService resourceService;
    private final IParseResultRequester<P> parseResultRequester;
    private final IAnalysisResultRequester<P, A> analysisResultRequester;
    private final IReferenceResolver<P, A> referenceResolver;

    private final FileObject resource;
    private final ILanguageImpl language;
    private final AbstractTextEditor editor;


    public SpoofaxHyperlinkDetector(IEclipseResourceService resourceService,
        IParseResultRequester<P> parseResultRequester, IAnalysisResultRequester<P, A> analysisResultRequester,
        IReferenceResolver<P, A> referenceResolver, FileObject resource, ILanguageImpl language, AbstractTextEditor editor) {
        this.resourceService = resourceService;
        this.parseResultRequester = parseResultRequester;
        this.analysisResultRequester = analysisResultRequester;
        this.referenceResolver = referenceResolver;

        this.resource = resource;
        this.language = language;
        this.editor = editor;
    }


    @Override public @Nullable IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean multiple) {
        if(!referenceResolver.available(language)) {
            return null;
        }
        
        final int offset = region.getOffset();
        final AnalysisFileResult<P, A> analysisResult = analysisResultRequester.get(resource);
        if(analysisResult != null) {
            return fromAnalyzed(offset, analysisResult);
        }
        final ParseResult<P> parseResult = parseResultRequester.get(resource);
        if(parseResult != null) {
            return fromParsed(offset, parseResult);
        }
        return null;
    }


    private @Nullable IHyperlink[] fromParsed(int offset, ParseResult<P> result) {
        try {
            final Resolution resolution = referenceResolver.resolve(offset, result);
            return createHyperlink(resolution);
        } catch(MetaborgException e) {
            logger.error("Reference resolution for {} failed unexpectedly", e, resource);
        }

        return null;
    }

    private @Nullable IHyperlink[] fromAnalyzed(int offset, AnalysisFileResult<P, A> result) {
        try {
            final Resolution resolution = referenceResolver.resolve(offset, result);
            return createHyperlink(resolution);
        } catch(MetaborgException e) {
            logger.error("Reference resolution for {} failed unexpectedly", e, resource);
        }

        return null;
    }

    private @Nullable IHyperlink[] createHyperlink(@Nullable Resolution resolution) {
        if(resolution == null) {
            return null;
        }
        final IHyperlink hyperlink = new SpoofaxHyperlink(resourceService, resolution, resource, editor);
        return new IHyperlink[] { hyperlink };
    }
}
