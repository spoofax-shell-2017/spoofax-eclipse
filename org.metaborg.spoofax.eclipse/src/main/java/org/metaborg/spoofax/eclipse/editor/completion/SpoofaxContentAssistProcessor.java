package org.metaborg.spoofax.eclipse.editor.completion;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.widgets.Display;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.completion.ICompletion;
import org.metaborg.core.completion.ICompletionService;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.processing.parse.IParseResultRequester;
import org.metaborg.core.syntax.IInputUnit;
import org.metaborg.core.syntax.IParseUnit;
import org.metaborg.core.unit.IInputUnitService;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spoofax.eclipse.SpoofaxPlugin;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.Subscription;
import rx.schedulers.Schedulers;

import com.google.common.collect.Iterables;

public class SpoofaxContentAssistProcessor<I extends IInputUnit, P extends IParseUnit> implements
    IContentAssistProcessor {
    
    private static final ILogger logger = LoggerUtils.logger(SpoofaxContentAssistProcessor.class);
    
    private final IInputUnitService<I> unitService;
    private final ICompletionService<ISpoofaxParseUnit> completionService;
    private final IParseResultRequester<I, P> parseResultRequester;
    private final FileObject resource;
    private final IDocument document;
    private final ILanguageImpl language;
    private final IInformationControlCreator informationControlCreator;

    private Subscription parseResultSubscription;
    private volatile ICompletionProposal[] cachedProposals;


    public SpoofaxContentAssistProcessor(IInputUnitService<I> unitService,
        IParseResultRequester<I, P> parseResultRequester, IInformationControlCreator informationControlCreator,
        FileObject resource, IDocument document, ILanguageImpl language) {
        this.unitService = unitService;
        this.completionService = SpoofaxPlugin.spoofax().completionService;
        this.parseResultRequester = parseResultRequester;
        this.informationControlCreator = informationControlCreator;
        this.resource = resource;
        this.document = document;
        this.language = language;
    }


    @Override public ICompletionProposal[] computeCompletionProposals(final ITextViewer viewer, final int offset) {
        if(cachedProposals != null) {
            final ICompletionProposal[] proposals = cachedProposals;
            cachedProposals = null;
            return proposals;
        }

        if(parseResultSubscription != null) {
            parseResultSubscription.unsubscribe();
        }

        final I input = unitService.inputUnit(resource, document.get(), language, null);


        parseResultSubscription = Observable.create(new OnSubscribe<Void>() {
            @Override public void call(final Subscriber<? super Void> subscriber) {
                if(subscriber.isUnsubscribed()) {
                    return;
                }
                // TODO: support dialects

                final ISpoofaxParseUnit parseResult =
                    (ISpoofaxParseUnit) parseResultRequester.request(input).toBlocking().first();

                if(subscriber.isUnsubscribed()) {
                    return;
                }
                cachedProposals = proposals(parseResult, viewer, offset);


                if(cachedProposals == null) {
                    return;
                }

                Display.getDefault().syncExec(new Runnable() {
                    @Override public void run() {
                        if(subscriber.isUnsubscribed()) {
                            return;
                        }
                        final ITextOperationTarget target = (ITextOperationTarget) viewer;
                        target.doOperation(ISourceViewer.CONTENTASSIST_PROPOSALS);
                    }
                });
            }
        }).observeOn(Schedulers.computation()).subscribeOn(Schedulers.computation()).subscribe();

        return null;
    }

    private ICompletionProposal[] proposals(ISpoofaxParseUnit parseResult, ITextViewer viewer, int offset) {
        final Iterable<ICompletion> completions;
        try {
            completions = completionService.get(offset, parseResult, false);
        } catch(MetaborgException e) {
            logger.error("Stratego completions framework failed at offset {}\n", offset);
            logger.error(e.toString());
            e.printStackTrace();
            return null;
        }

        final int numCompletions = Iterables.size(completions);
        final ICompletionProposal[] proposals = new ICompletionProposal[numCompletions];
        int i = 0;
        for(ICompletion completion : completions) {
            proposals[i] =
                new SpoofaxCompletionProposal(viewer, offset, completion, parseResult.source(), parseResult.input()
                    .langImpl(), informationControlCreator);
            ++i;
        }
        return proposals;
    }

    @Override public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
        return null;
    }

    @Override public char[] getCompletionProposalAutoActivationCharacters() {
        return null;
    }

    @Override public char[] getContextInformationAutoActivationCharacters() {
        return null;
    }

    @Override public String getErrorMessage() {
        return null;
    }

    @Override public IContextInformationValidator getContextInformationValidator() {
        return null;
    }
}
