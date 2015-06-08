package org.metaborg.spoofax.eclipse.editor;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.metaborg.spoofax.core.SpoofaxException;
import org.metaborg.spoofax.core.analysis.AnalysisFileResult;
import org.metaborg.spoofax.core.processing.analyze.IAnalysisResultRequester;
import org.metaborg.spoofax.core.tracing.Hover;
import org.metaborg.spoofax.core.tracing.IHoverService;
import org.metaborg.spoofax.eclipse.util.RegionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpoofaxTextHover<P, A> implements ITextHover {
    private static final Logger logger = LoggerFactory.getLogger(SpoofaxTextHover.class);

    private final IAnalysisResultRequester<P, A> analysisResultRequester;
    private final IHoverService<P, A> hoverService;

    private final FileObject resource;


    public SpoofaxTextHover(IAnalysisResultRequester<P, A> analysisResultRequester, IHoverService<P, A> hoverService,
        FileObject resource) {
        this.analysisResultRequester = analysisResultRequester;
        this.hoverService = hoverService;

        this.resource = resource;
    }


    @Override public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
        final Hover hover = hover(hoverRegion);
        if(hover == null) {
            return null;
        }
        return hover.text;
    }

    @Override public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
        // GTODO: prevent double call to hover service, requires knowledge of which method is called first.
        final Hover hover = hover(new Region(offset, 0));
        if(hover == null) {
            return null;
        }
        return RegionUtils.fromCore(hover.region);
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
        } catch(SpoofaxException e) {
            final String message = String.format("Hover information creation for %s failed unexpectedly", resource);
            logger.error(message, e);
        }

        return null;
    }
}
