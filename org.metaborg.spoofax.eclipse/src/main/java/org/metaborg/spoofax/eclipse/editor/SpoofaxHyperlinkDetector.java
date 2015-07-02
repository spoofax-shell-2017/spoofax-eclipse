package org.metaborg.spoofax.eclipse.editor;


import org.apache.commons.vfs2.FileObject;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.analysis.AnalysisFileResult;
import org.metaborg.core.build.processing.analyze.IAnalysisResultRequester;
import org.metaborg.core.tracing.IReferenceResolver;
import org.metaborg.core.tracing.Resolution;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpoofaxHyperlinkDetector<P, A> extends AbstractHyperlinkDetector {
    private static final Logger logger = LoggerFactory.getLogger(SpoofaxHyperlinkDetector.class);

    private final IEclipseResourceService resourceService;
    private final IAnalysisResultRequester<P, A> analysisResultRequester;
    private final IReferenceResolver<P, A> referenceResolver;

    private final FileObject resource;
    private final AbstractTextEditor editor;


    public SpoofaxHyperlinkDetector(IEclipseResourceService resourceService,
        IAnalysisResultRequester<P, A> analysisResultRequester, IReferenceResolver<P, A> referenceResolver,
        FileObject resource, AbstractTextEditor editor) {
        this.resourceService = resourceService;
        this.analysisResultRequester = analysisResultRequester;
        this.referenceResolver = referenceResolver;

        this.resource = resource;
        this.editor = editor;
    }


    @Override public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean multiple) {
        final AnalysisFileResult<P, A> result = analysisResultRequester.get(resource);
        if(result == null) {
            return null;
        }

        try {
            final Resolution resolution = referenceResolver.resolve(region.getOffset(), result);
            if(resolution == null) {
                return null;
            }
            final IHyperlink hyperlink = new SpoofaxHyperlink(resourceService, resolution, resource, editor);
            return new IHyperlink[] { hyperlink };
        } catch(MetaborgException e) {
            final String message = String.format("Reference resolution for %s failed unexpectedly", resource);
            logger.error(message, e);
        }

        return null;
    }
}
