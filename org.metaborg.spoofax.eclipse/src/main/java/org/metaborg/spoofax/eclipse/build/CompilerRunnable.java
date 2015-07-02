package org.metaborg.spoofax.eclipse.build;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.metaborg.core.analysis.AnalysisFileResult;
import org.metaborg.core.analysis.AnalysisResult;
import org.metaborg.core.context.IContext;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.messages.MessageFactory;
import org.metaborg.core.transform.CompileGoal;
import org.metaborg.core.transform.ITransformer;
import org.metaborg.core.transform.TransformerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spoofax.interpreter.terms.IStrategoTerm;

public class CompilerRunnable implements IWorkspaceRunnable {
    private static final Logger logger = LoggerFactory.getLogger(CompilerRunnable.class);

    private final ITransformer<IStrategoTerm, IStrategoTerm, IStrategoTerm> transformer;

    private final FileObject projectResource;
    private final Set<FileName> removedResources;
    private final Iterable<Entry<IContext, AnalysisResult<IStrategoTerm, IStrategoTerm>>> analysisResults;

    private final Collection<IMessage> extraMessages;


    CompilerRunnable(ITransformer<IStrategoTerm, IStrategoTerm, IStrategoTerm> transformer, FileObject projectResource,
        Set<FileName> removedResources,
        Iterable<Entry<IContext, AnalysisResult<IStrategoTerm, IStrategoTerm>>> analysisResults,
        Collection<IMessage> extraMessages) {
        this.transformer = transformer;

        this.projectResource = projectResource;
        this.removedResources = removedResources;
        this.analysisResults = analysisResults;

        this.extraMessages = extraMessages;
    }


    @Override public void run(IProgressMonitor workspaceMonitor) throws CoreException {
        final CompileGoal compileGoal = new CompileGoal();
        for(Entry<IContext, AnalysisResult<IStrategoTerm, IStrategoTerm>> entry : analysisResults) {
            final IContext context = entry.getKey();
            if(!transformer.available(compileGoal, context)) {
                logger.debug("No compilation required for {}", context.language());
                continue;
            }
            final AnalysisResult<IStrategoTerm, IStrategoTerm> analysisResult = entry.getValue();
            synchronized(context) {
                for(AnalysisFileResult<IStrategoTerm, IStrategoTerm> fileResult : analysisResult.fileResults) {
                    if(removedResources.contains(fileResult.source.getName())) {
                        // Don't compile removed resources. Only analysis results contain removed resources.
                        continue;
                    }

                    if(fileResult.result == null) {
                        logger.warn("Input result for {} is null, cannot compile it", fileResult.source);
                        continue;
                    }

                    try {
                        transformer.transform(fileResult, context, compileGoal);
                    } catch(TransformerException e) {
                        logger.error("Compilation failed", e);
                        extraMessages
                            .add(MessageFactory.newBuilderErrorAtTop(projectResource, "Compilation failed", e));
                    }
                }
                // GTODO: also compile any affected sources
            }
        }
    }
}