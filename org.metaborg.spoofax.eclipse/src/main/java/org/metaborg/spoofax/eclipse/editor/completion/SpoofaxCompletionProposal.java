package org.metaborg.spoofax.eclipse.editor.completion;

import java.util.Collection;
import java.util.LinkedList;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.text.link.ProposalPosition;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.completion.ICompletion;
import org.metaborg.core.completion.ICompletionItem;
import org.metaborg.core.completion.ICompletionService;
import org.metaborg.core.completion.IPlaceholderCompletionItem;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.syntax.ISyntaxService;
import org.metaborg.core.syntax.ParseException;
import org.metaborg.core.tracing.ITracingService;
import org.metaborg.core.unit.IInputUnitService;
import org.metaborg.spoofax.core.completion.PlaceholderCompletionItem;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxTransformUnit;
import org.metaborg.spoofax.eclipse.SpoofaxPlugin;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.visitor.AStrategoTermVisitor;
import org.spoofax.terms.visitor.IStrategoTermVisitor;
import org.spoofax.terms.visitor.StrategoTermVisitee;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

public class SpoofaxCompletionProposal implements ICompletionProposal {
    private static class CompletionData {
        public final String text;
        public final Multimap<String, ProposalPosition> placeholders;
        public final int cursorSequence;
        public final int cursorPosition;
        public int finalSequence = 0;


        public CompletionData(IDocument document, String text, ITextViewer viewer, int offset, ICompletion completion,
            ICompletionService<ISpoofaxParseUnit> completionService, ISpoofaxParseUnit parseResult) {
            placeholders = ArrayListMultimap.create();
            this.text = text;

            int sequenceNumber = 0;
            int sequenceSize = Iterables.size(completion.items());

            for(ICompletionItem item : completion.items()) {
                if(item instanceof IPlaceholderCompletionItem) {
                    final IPlaceholderCompletionItem placeholderItem = (IPlaceholderCompletionItem) item;
                    if(placeholderItem.startOffset() < offset) {
                        sequenceNumber++;
                    }
                }
            }

            sequenceNumber = sequenceSize - sequenceNumber;


            for(ICompletionItem item : completion.items()) {
                if(item instanceof IPlaceholderCompletionItem) {
                    final IPlaceholderCompletionItem placeholderItem = (IPlaceholderCompletionItem) item;
                    final String name = placeholderItem.name();

                    final int placeholderLenght =
                        (placeholderItem.endOffset() != placeholderItem.startOffset()) ? placeholderItem.endOffset()
                            - placeholderItem.startOffset() + 1 : 0;
                    final int cursorPosition =
                        (placeholderItem.endOffset() != placeholderItem.startOffset())
                            ? placeholderItem.startOffset() + 1 : placeholderItem.startOffset();

                    final ProposalPosition position =
                        new ProposalPosition(document, placeholderItem.startOffset(), placeholderLenght,
                            (sequenceNumber < sequenceSize) ? sequenceNumber++ : 0, getProposals(completionService,
                                parseResult, viewer, cursorPosition, placeholderItem));
                    placeholders.put(name, position);
                }
            }

            cursorSequence = finalSequence;
            cursorPosition = 0;

        }

        private ICompletionProposal[] getProposals(ICompletionService<ISpoofaxParseUnit> completionService,
            ISpoofaxParseUnit parseResult, ITextViewer viewer, int offset, IPlaceholderCompletionItem placeholderItem) {
            // call the completion proposer to calculate the proposals
            final Iterable<ICompletion> completions;
            try {
                completions = completionService.get(offset, parseResult, true);
            } catch(MetaborgException e) {
                return null;
            }

            final int numCompletions = Iterables.size(completions);
            final ICompletionProposal[] proposals = new ICompletionProposal[numCompletions];
            int i = 0;
            for(ICompletion completion : completions) {
                completion.setNested(true && !placeholderItem.optional());
                proposals[i] =
                    new SpoofaxCompletionProposal(viewer, offset, completion, parseResult.source(), parseResult.input()
                        .langImpl());
                ++i;
            }
            return proposals;
        }

    }

    private static final ILogger logger = LoggerUtils.logger(SpoofaxCompletionProposal.class);
    private final ITextViewer textViewer;
    private int offset;
    private ICompletion completion;
    private final ICompletionService<ISpoofaxParseUnit> completionService;
    private final IInputUnitService<ISpoofaxInputUnit> unitService;
    private final ISyntaxService<ISpoofaxInputUnit, ISpoofaxParseUnit> syntaxService;
    private final ITracingService<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit, ISpoofaxTransformUnit<?>, IStrategoTerm> tracingService;
    private final FileObject source;
    private final ILanguageImpl language;

    private CompletionData data;

    public SpoofaxCompletionProposal(ITextViewer textViewer, int offset, ICompletion completion, FileObject source,
        ILanguageImpl language) {
        this.textViewer = textViewer;
        this.offset = offset;
        this.completion = completion;
        this.completionService = SpoofaxPlugin.spoofax().completionService;
        this.unitService = SpoofaxPlugin.spoofax().unitService;
        this.syntaxService = SpoofaxPlugin.spoofax().syntaxService;
        this.tracingService = SpoofaxPlugin.spoofax().tracingService;
        this.source = source;
        this.language = language;
    }

    @Override public void apply(IDocument document) {

        int startOffset;
        int endOffset;
        int endingCursorOffset;

        // if completion is nested, replace the selected text
        if(completion.isNested()) {
            startOffset = textViewer.getSelectedRange().x;
            endOffset = startOffset + textViewer.getSelectedRange().y;
            offset = startOffset;
        } else { // if not nested, then replace with the completion offsets
            startOffset = completion.startOffset();
            endOffset = completion.endOffset();
        }

        // final text after applying completions
        String finalText = document.get().substring(0, startOffset);
        finalText += completion.text();
        finalText += document.get().substring(endOffset);


        endingCursorOffset = finalText.length();

        // re-parse to get the new AST to calculate nested completions
        ISpoofaxParseUnit completedParseResult = null;

        try {
            final ISpoofaxInputUnit input =
                (ISpoofaxInputUnit) unitService.inputUnit(source, finalText, language, null);
            completedParseResult = syntaxService.parse(input);
        } catch(ParseException e1) {
            e1.printStackTrace();
        }

        final Collection<ICompletionItem> completionItems = createItemsFromAST(completedParseResult);
        completion.setItems(completionItems);

        this.data =
            new CompletionData(document, finalText, textViewer, startOffset, completion, completionService,
                completedParseResult);

        try {
            document.replace(0, document.getLength(), finalText);

            if(!data.placeholders.isEmpty()) {
                final LinkedModeModel model = new LinkedModeModel();
                for(Collection<ProposalPosition> positions : data.placeholders.asMap().values()) {
                    final LinkedPositionGroup group = new LinkedPositionGroup();
                    for(ProposalPosition position : positions) {
                        group.addPosition(position);
                    }
                    model.addGroup(group);
                }
                model.forceInstall();
                final LinkedModeUI ui = new LinkedModeUI(model, textViewer);
                ui.setExitPosition(textViewer, endingCursorOffset, 0, data.cursorSequence);
                ui.enter();
            }
        } catch(BadLocationException e) {
            final String message =
                String.format("Cannot apply completion at offset %s, length %s", offset, data.text.length());
            logger.error(message, e);
        }
    }

    private Collection<ICompletionItem> createItemsFromAST(ISpoofaxParseUnit completedParseResult) {


        final Collection<ICompletionItem> result = new LinkedList<ICompletionItem>();

        final IStrategoTermVisitor visitor = new AStrategoTermVisitor() {

            @Override public boolean visit(IStrategoTerm term) {
                int startOffset = tracingService.location(term).region().startOffset();
                int endOffset = tracingService.location(term).region().endOffset();

                // if it is a placeholder (check constructor)
                if(term instanceof IStrategoAppl) {
                    String constructor = ((IStrategoAppl) term).getConstructor().getName();

                    if(constructor.contains("-Plhdr")) {
                        String placeholderName = constructor.substring(0, constructor.length() - 6);
                        result.add(new PlaceholderCompletionItem(placeholderName, startOffset, endOffset, false));
                    }
                    // else if(startOffset > endOffset){ // optional
                    // result.add(new PlaceholderCompletionItem("", startOffset, startOffset, true));
                    // }


                }
                // else if(startOffset > endOffset){ // empty list
                // result.add(new PlaceholderCompletionItem("", startOffset, startOffset, true));
                // }



                return true;
            }
        };
        StrategoTermVisitee.topdown(visitor, completedParseResult.ast());

        return result;
    }

    @Override public Point getSelection(IDocument document) {
        if(data.placeholders.isEmpty()) {
            return new Point(data.cursorPosition, 0);
        }

        // There are placeholders, let linked mode take care of moving the cursor to the first placeholder. Returning
        // null signals that selection should not be changed by the completion proposal.
        return null;
    }

    @Override public String getAdditionalProposalInfo() {
        System.out.println(completion.text());
        return "<code>" + completion.text() + "</code>";
    }

    @Override public String getDisplayString() {
        return completion.toString();
    }

    @Override public Image getImage() {
        return null;
    }

    @Override public IContextInformation getContextInformation() {
        return null;
    }


}
