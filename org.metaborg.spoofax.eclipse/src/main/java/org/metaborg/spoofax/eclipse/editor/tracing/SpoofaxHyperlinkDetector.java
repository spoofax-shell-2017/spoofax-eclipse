package org.metaborg.spoofax.eclipse.editor.tracing;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.analysis.IAnalyzeUnit;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.processing.analyze.IAnalysisResultRequester;
import org.metaborg.core.processing.parse.IParseResultRequester;
import org.metaborg.core.syntax.IInputUnit;
import org.metaborg.core.syntax.IParseUnit;
import org.metaborg.core.tracing.IResolverService;
import org.metaborg.core.tracing.Resolution;
import org.metaborg.spoofax.eclipse.editor.IEclipseEditor;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.eclipse.util.Nullable;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

public class SpoofaxHyperlinkDetector<I extends IInputUnit, P extends IParseUnit, A extends IAnalyzeUnit, F>
    extends AbstractHyperlinkDetector {
    private static final ILogger logger = LoggerUtils.logger(SpoofaxHyperlinkDetector.class);

    private final IEclipseResourceService resourceService;
    private final IParseResultRequester<I, P> parseResultRequester;
    private final IAnalysisResultRequester<I, A> analysisResultRequester;
    private final IResolverService<P, A> resolverService;

    private final FileObject resource;
    private final ILanguageImpl language;
    private final IEclipseEditor<F> editor;


    public SpoofaxHyperlinkDetector(IEclipseResourceService resourceService,
        IParseResultRequester<I, P> parseResultRequester, IAnalysisResultRequester<I, A> analysisResultRequester,
        IResolverService<P, A> resolverService, FileObject resource, ILanguageImpl language, IEclipseEditor<F> editor) {
        this.resourceService = resourceService;
        this.parseResultRequester = parseResultRequester;
        this.analysisResultRequester = analysisResultRequester;
        this.resolverService = resolverService;

        this.resource = resource;
        this.language = language;
        this.editor = editor;
    }


    @Override public @Nullable IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean multiple) {
        if(!resolverService.available(language) || editor.editorIsUpdating()) {
            logger.info("Skipping reference resolution");
            return null;
        }

        final int offset = region.getOffset();
        final A analysisResult = analysisResultRequester.get(resource);
        if(analysisResult != null) {
            return fromAnalyzed(offset, analysisResult);
        }
        final P parseResult = parseResultRequester.get(resource);
        if(parseResult != null) {
            return fromParsed(offset, parseResult);
        }
        return null;
    }


    private @Nullable IHyperlink[] fromParsed(int offset, P result) {
        try {
            final Resolution resolution = resolverService.resolve(offset, result);
            return createHyperlink(resolution);
        } catch(MetaborgException e) {
            logger.error("Reference resolution for {} failed unexpectedly", e, resource);
        }

        return null;
    }

    private @Nullable IHyperlink[] fromAnalyzed(int offset, A result) {
        try {
            final Resolution resolution = resolverService.resolve(offset, result);
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
