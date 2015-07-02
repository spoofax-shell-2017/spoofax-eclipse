package org.metaborg.spoofax.eclipse.editor;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModelExtension2;
import org.eclipse.jface.text.source.ISourceViewerExtension2;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.analysis.AnalysisFileResult;
import org.metaborg.core.build.processing.analyze.IAnalysisResultRequester;
import org.metaborg.core.tracing.Hover;
import org.metaborg.core.tracing.IHoverService;
import org.metaborg.util.iterators.Iterables2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpoofaxTextHover<P, A> implements ITextHover {
    private static final Logger logger = LoggerFactory.getLogger(SpoofaxTextHover.class);

    private final IAnalysisResultRequester<P, A> analysisResultRequester;
    private final IHoverService<P, A> hoverService;

    private final FileObject resource;
    private final ISourceViewerExtension2 sourceViewer;


    public SpoofaxTextHover(IAnalysisResultRequester<P, A> analysisResultRequester, IHoverService<P, A> hoverService,
        FileObject resource, ISourceViewerExtension2 sourceViewer) {
        this.analysisResultRequester = analysisResultRequester;
        this.hoverService = hoverService;

        this.resource = resource;
        this.sourceViewer = sourceViewer;
    }


    @Override public String getHoverInfo(ITextViewer viewer, IRegion region) {
        final StringBuilder stringBuilder = annotationHover(region);
        final Hover hover = hover(region);
        if(hover != null) {
            stringBuilder.append("<br/>");
            stringBuilder.append(hover.text);
        }
        return stringBuilder.toString();
    }

    @Override public IRegion getHoverRegion(ITextViewer viewer, int offset) {
        return new Region(offset, 1);
    }


    private Hover hover(IRegion region) {
        final AnalysisFileResult<P, A> result = analysisResultRequester.get(resource);
        if(result == null) {
            return null;
        }

        try {
            final Hover hover = hoverService.hover(region.getOffset(), result);
            if(hover == null) {
                return null;
            }
            return hover;
        } catch(MetaborgException e) {
            final String message = String.format("Hover information creation for %s failed unexpectedly", resource);
            logger.error(message, e);
        }

        return null;
    }

    @SuppressWarnings("unchecked") private StringBuilder annotationHover(IRegion region) {
        final IAnnotationModelExtension2 annotationModel =
            (IAnnotationModelExtension2) sourceViewer.getVisualAnnotationModel();
        final StringBuilder stringBuilder = new StringBuilder();
        for(Annotation annotation : Iterables2.<Annotation>fromOnce(annotationModel.getAnnotationIterator(
            region.getOffset(), region.getLength(), true, true))) {
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
