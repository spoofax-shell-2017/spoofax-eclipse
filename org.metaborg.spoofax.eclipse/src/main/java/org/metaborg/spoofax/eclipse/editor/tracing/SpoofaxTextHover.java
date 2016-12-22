package org.metaborg.spoofax.eclipse.editor.tracing;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModelExtension2;
import org.eclipse.jface.text.source.ISourceViewerExtension2;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.analysis.IAnalyzeUnit;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.processing.analyze.IAnalysisResultRequester;
import org.metaborg.core.processing.parse.IParseResultRequester;
import org.metaborg.core.syntax.IInputUnit;
import org.metaborg.core.syntax.IParseUnit;
import org.metaborg.core.tracing.Hover;
import org.metaborg.core.tracing.IHoverService;
import org.metaborg.spoofax.eclipse.editor.IEclipseEditor;
import org.metaborg.spoofax.eclipse.util.Nullable;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

public class SpoofaxTextHover<I extends IInputUnit, P extends IParseUnit, A extends IAnalyzeUnit, F>
    implements ITextHover {
    private static final ILogger logger = LoggerUtils.logger(SpoofaxTextHover.class);

    private final IParseResultRequester<I, P> parseResultRequester;
    private final IAnalysisResultRequester<I, A> analysisResultRequester;
    private final IHoverService<P, A> hoverService;

    private final FileObject resource;
    private final ILanguageImpl language;
    private final IEclipseEditor<F> editor;
    private final ISourceViewerExtension2 sourceViewer;


    public SpoofaxTextHover(IParseResultRequester<I, P> parseResultRequester,
        IAnalysisResultRequester<I, A> analysisResultRequester, IHoverService<P, A> hoverService, FileObject resource,
        ILanguageImpl language, IEclipseEditor<F> editor, ISourceViewerExtension2 sourceViewer) {
        this.parseResultRequester = parseResultRequester;
        this.analysisResultRequester = analysisResultRequester;
        this.hoverService = hoverService;

        this.resource = resource;
        this.language = language;
        this.editor = editor;
        this.sourceViewer = sourceViewer;
    }


    @Override public String getHoverInfo(ITextViewer viewer, IRegion region) {
        final StringBuilder stringBuilder = annotationHover(region);
        if(hoverService.available(language) && !editor.editorIsUpdating()) {
            final int offset = region.getOffset();

            Hover hover = null;
            final A analysisResult = analysisResultRequester.get(resource);
            if(analysisResult != null) {
                hover = fromAnalyzed(offset, analysisResult);
            }
            if(hover == null) {
                final P parseResult = parseResultRequester.get(resource);
                if(parseResult != null) {
                    hover = fromParsed(offset, parseResult);
                }
            }

            if(hover != null) {
                stringBuilder.append("<br/>");
                stringBuilder.append(hover.text);
            }
        }
        return stringBuilder.toString();
    }

    @Override public IRegion getHoverRegion(ITextViewer viewer, int offset) {
        return new Region(offset, 1);
    }


    private @Nullable Hover fromParsed(int offset, @Nullable P result) {
        try {
            final Hover hover = hoverService.hover(offset, result);
            return hover;
        } catch(MetaborgException e) {
            logger.error("Getting hover tooltip information for {} failed unexpectedly", e, resource);
        }

        return null;
    }

    private @Nullable Hover fromAnalyzed(int offset, @Nullable A result) {
        try {
            final Hover hover = hoverService.hover(offset, result);
            return hover;
        } catch(MetaborgException e) {
            logger.error("Getting hover tooltip information for {} failed unexpectedly", e, resource);
        }

        return null;
    }


    private StringBuilder annotationHover(IRegion region) {
        final IAnnotationModelExtension2 annotationModel =
            (IAnnotationModelExtension2) sourceViewer.getVisualAnnotationModel();
        final StringBuilder stringBuilder = new StringBuilder();
        if(annotationModel == null) {
            return stringBuilder;
        }
        final Iterable<Annotation> annotations = Iterables2.<Annotation>fromOnce(
            annotationModel.getAnnotationIterator(region.getOffset(), region.getLength(), true, true));
        for(Annotation annotation : annotations) {
            // Ignore certain annotations types.
            switch(annotation.getType()) {
                case "org.eclipse.ui.workbench.texteditor.quickdiffDeletion":
                case "org.eclipse.ui.workbench.texteditor.quickdiffChange":
                case "org.eclipse.ui.workbench.texteditor.quickdiffAddition":
                case "org.eclipse.ui.workbench.texteditor.quickdiffUnchanged":
                    continue;
                default:
            }
            stringBuilder.append(annotation.getText());
            stringBuilder.append("<br/>");
        }
        return stringBuilder;
    }
}
