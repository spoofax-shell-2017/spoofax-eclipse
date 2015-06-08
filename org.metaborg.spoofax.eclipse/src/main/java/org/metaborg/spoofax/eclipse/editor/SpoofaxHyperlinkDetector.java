package org.metaborg.spoofax.eclipse.editor;


import java.io.File;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.metaborg.spoofax.core.SpoofaxException;
import org.metaborg.spoofax.core.analysis.AnalysisFileResult;
import org.metaborg.spoofax.core.context.IContext;
import org.metaborg.spoofax.core.context.IContextService;
import org.metaborg.spoofax.core.language.ILanguage;
import org.metaborg.spoofax.core.messages.ISourceLocation;
import org.metaborg.spoofax.core.messages.ISourceRegion;
import org.metaborg.spoofax.core.processing.analyze.ISpoofaxAnalysisResultRequester;
import org.metaborg.spoofax.core.stratego.IStrategoRuntimeService;
import org.metaborg.spoofax.core.stratego.StrategoLocalPath;
import org.metaborg.spoofax.core.stratego.StrategoRuntimeUtils;
import org.metaborg.spoofax.core.terms.ITermFactoryService;
import org.metaborg.spoofax.core.tracing.spoofax.ISpoofaxTracingService;
import org.metaborg.spoofax.eclipse.SpoofaxPlugin;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.eclipse.util.EditorUtils;
import org.metaborg.spoofax.eclipse.util.RegionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.strategoxt.HybridInterpreter;

public class SpoofaxHyperlinkDetector extends AbstractHyperlinkDetector {
    private static final Logger logger = LoggerFactory.getLogger(SpoofaxHyperlinkDetector.class);

    private final ISpoofaxTracingService tracingService;
    private final ISpoofaxAnalysisResultRequester analysisResultRequester;
    private final IStrategoRuntimeService strategoRuntimeService;
    private final IContextService contextService;
    private final ITermFactoryService termFactoryService;
    private final IEclipseResourceService resourceService;
    private final StrategoLocalPath localPath;

    private final FileObject resource;
    private final ILanguage language;
    private final AbstractTextEditor editor;

    private final IContext context;
    private final HybridInterpreter runtime;
    private final ITermFactory termFactory;


    public SpoofaxHyperlinkDetector(FileObject resource, ILanguage language, AbstractTextEditor editor)
        throws SpoofaxException {
        this.tracingService = SpoofaxPlugin.injector().getInstance(ISpoofaxTracingService.class);
        this.analysisResultRequester = SpoofaxPlugin.injector().getInstance(ISpoofaxAnalysisResultRequester.class);
        this.strategoRuntimeService = SpoofaxPlugin.injector().getInstance(IStrategoRuntimeService.class);
        this.contextService = SpoofaxPlugin.injector().getInstance(IContextService.class);
        this.termFactoryService = SpoofaxPlugin.injector().getInstance(ITermFactoryService.class);
        this.resourceService = SpoofaxPlugin.injector().getInstance(IEclipseResourceService.class);
        this.localPath = SpoofaxPlugin.injector().getInstance(StrategoLocalPath.class);

        this.resource = resource;
        this.language = language;
        this.editor = editor;

        context = contextService.get(resource, language);
        runtime = strategoRuntimeService.runtime(context);
        termFactory = termFactoryService.get(language);
    }


    @Override public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean multiple) {
        final AnalysisFileResult<IStrategoTerm, IStrategoTerm> result = analysisResultRequester.get(resource);
        if(result == null || result.result == null) {
            return null;
        }

        try {
            final File localContextLocation = resourceService.localFile(context.location());
            final File localResource = resourceService.localPath(resource);
            if(localContextLocation == null || localResource == null) {
                return null;
            }
            final IStrategoString path = localPath.localResourceTerm(localResource, localContextLocation);
            final IStrategoString contextPath = localPath.localLocationTerm(localContextLocation);

            final Iterable<IStrategoTerm> inRegion = tracingService.toAnalyzed(result, RegionUtils.toCore(region));
            for(IStrategoTerm term : inRegion) {
                final IStrategoTerm inputTerm =
                    termFactory.makeTuple(term, termFactory.makeTuple(), result.result, path, contextPath);
                final IStrategoTerm targetTerm = StrategoRuntimeUtils.invoke(runtime, inputTerm, "editor-resolve");
                if(targetTerm == null) {
                    continue;
                }

                final ISourceLocation targetLocation = tracingService.fromParsed(targetTerm);
                if(targetLocation == null || targetLocation.resource() == null) {
                    continue;
                }
                final ISourceRegion targetRegion = targetLocation.region();
                final FileObject targetResource = targetLocation.resource();

                final ISourceLocation highlightLocation = tracingService.fromAnalyzed(term);
                if(highlightLocation == null) {
                    continue;
                }

                final IHyperlink hyperLink = new IHyperlink() {
                    @Override public void open() {
                        if(targetResource.getName().equals(resource.getName())) {
                            EditorUtils.editorFocus(editor, targetRegion.startOffset());
                        } else {
                            final IResource eclipseResource = resourceService.unresolve(targetResource);
                            if(eclipseResource != null && eclipseResource instanceof IFile) {
                                final IFile file = (IFile) eclipseResource;
                                EditorUtils.openEditor(file, targetRegion.startOffset());
                            }
                        }
                    }

                    @Override public String getTypeLabel() {
                        return null;
                    }

                    @Override public String getHyperlinkText() {
                        return null;
                    }

                    @Override public IRegion getHyperlinkRegion() {
                        return RegionUtils.fromCore(highlightLocation.region());
                    }
                };
                return new IHyperlink[] { hyperLink };
            }
        } catch(SpoofaxException e) {
            logger.error("Cannot do shit", e);
        }

        return null;
    }
}
