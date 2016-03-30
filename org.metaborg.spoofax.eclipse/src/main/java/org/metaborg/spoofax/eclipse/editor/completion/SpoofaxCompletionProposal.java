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
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.text.link.ProposalPosition;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.completion.ICompletion;
import org.metaborg.core.completion.ICompletionItem;
import org.metaborg.core.completion.ICompletionService;
import org.metaborg.core.completion.ICursorCompletionItem;
import org.metaborg.core.completion.IPlaceholderCompletionItem;
import org.metaborg.core.completion.ITextCompletionItem;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.syntax.IInputUnit;
import org.metaborg.core.syntax.IParseUnit;
import org.metaborg.core.syntax.ISyntaxService;
import org.metaborg.core.syntax.ParseException;
import org.metaborg.core.unit.IInputUnitService;
import org.metaborg.spoofax.core.completion.PlaceholderCompletionItem;
import org.metaborg.spoofax.core.completion.TextCompletionItem;
import org.metaborg.spoofax.core.syntax.JSGLRParserConfiguration;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

public class SpoofaxCompletionProposal<I extends IInputUnit, P extends IParseUnit> implements ICompletionProposal {
    private static class CompletionData<I extends IInputUnit, P extends IParseUnit> {
        public final String text;
        public final Multimap<String, ProposalPosition> placeholders;
        public final int cursorPosition;
        public final int cursorSequence;
        public static int sequence = 0;


        public CompletionData(IDocument document, ITextViewer viewer, int offset, ICompletion completion,
            ICompletionService<P> completionService, IInputUnitService<I> unitService, ISyntaxService<I, P> syntaxService, P parseResult) {
            placeholders = ArrayListMultimap.create();

            final StringBuilder stringBuilder = new StringBuilder();
            String prefix = document.get();
            prefix = prefix.substring(0, offset);

            int placeholdersPrefix = countPlaceholders(prefix);
            int placeholdersCompletion = countPlaceholders(completion);


            sequence = placeholdersCompletion - placeholdersPrefix;

            int textOffset = 0;
            int curCursorOffset = -1;
            int curCursorSequence = -1;
            for(ICompletionItem item : completion.items()) {
                if(item instanceof ITextCompletionItem) {
                    final ITextCompletionItem textItem = (ITextCompletionItem) item;
                    final String itemText = textItem.text();
                    stringBuilder.append(itemText);
                    textOffset += itemText.length();
                } else if(item instanceof IPlaceholderCompletionItem) {
                    final IPlaceholderCompletionItem placeholderItem = (IPlaceholderCompletionItem) item;
                    final String itemText = "[[" + placeholderItem.placeholderText() + "]]";
                    final int textLength = itemText.length();
                    final String name = placeholderItem.name();
                    stringBuilder.append(itemText);
                    final ProposalPosition position =
                        new ProposalPosition(document, textOffset, textLength, getNextSequence(placeholdersCompletion),
                            getProposals(completionService, unitService, syntaxService, parseResult, viewer, textOffset,
                                placeholderItem));
                    placeholders.put(name, position);
                    textOffset += itemText.length();
                } else if(item instanceof ICursorCompletionItem) {
                    curCursorOffset = textOffset;
                    curCursorSequence = sequence++;
                }
            }

            if(curCursorOffset == -1) {
                curCursorOffset = textOffset;
                curCursorSequence = sequence++;
            }

            text = stringBuilder.toString();
            cursorPosition = curCursorOffset;
            cursorSequence = curCursorSequence;
        }

        private ICompletionProposal[] getProposals(ICompletionService<P> completionService,
            IInputUnitService<I> unitService, ISyntaxService<I, P> syntaxService, P parseResult, ITextViewer viewer, int offset,
            IPlaceholderCompletionItem placeholderItem) {
            // call the completion proposer to calculate the proposals
            final Iterable<ICompletion> completions;
            try {
                completions = completionService.get(offset + 1, parseResult);
            } catch(MetaborgException e) {
                return null;
            }

            final int numCompletions = Iterables.size(completions);
            final ICompletionProposal[] proposals = new ICompletionProposal[numCompletions];
            int i = 0;
            for(ICompletion completion : completions) {
                completion.setNested(true);
                proposals[i] =
                    new SpoofaxCompletionProposal<>(viewer, offset, completion, parseResult.source(), parseResult.input().langImpl(),
                        completionService, unitService, syntaxService);
                ++i;
            }
            return proposals;
        }

        public int getNextSequence(int max) {

            if(sequence < max - 1) {
                return sequence++;
            }

            int result = max - 1;
            sequence = 0;

            return result;

        }

        public int countPlaceholders(String input) {
            int numberOfPlaceholders = 0;
            for(int i = 0; i < input.length(); i++) {
                if(input.charAt(i) == '[') {
                    i++;
                    if(input.charAt(i) == '[') {
                        i++;

                        while(i < input.length() && input.charAt(i) == '[')
                            // nested brackets
                            i++;

                        while(i < input.length() && input.charAt(i) != ']') {
                            String charAti = String.valueOf(input.charAt(i));

                            if(!charAti.matches("[a-zA-Z_]")) { // not placeholder: abort
                                break;
                            }

                            i++;
                        }

                        if(i >= input.length() || input.charAt(i) != ']')
                            continue;
                        i++;

                        numberOfPlaceholders++;
                        continue;
                    }
                }
            }
            return numberOfPlaceholders;
        }

        public int countPlaceholders(ICompletion completion) {
            int numberOfPlaceholders = 0;
            for(ICompletionItem i : completion.items()) {
                if(i instanceof PlaceholderCompletionItem)
                    numberOfPlaceholders++;
            }
            return numberOfPlaceholders;

        }
    }

    private static final ILogger logger = LoggerUtils.logger(SpoofaxCompletionProposal.class);
    private final ITextViewer textViewer;
    private int offset;
    private ICompletion completion;
    private final ICompletionService<P> completionService;
    private final IInputUnitService<I> unitService;
    private final ISyntaxService<I, P> syntaxService;
    private final FileObject source;
    private final ILanguageImpl language;

    private CompletionData<I, P> data;

    public SpoofaxCompletionProposal(ITextViewer textViewer, int offset, ICompletion completion, FileObject source,
        ILanguageImpl language, ICompletionService<P> completionService, IInputUnitService<I> unitService,ISyntaxService<I, P> syntaxService) {
        this.textViewer = textViewer;
        this.offset = offset;
        this.completion = completion;
        this.completionService = completionService;
        this.unitService = unitService;
        this.syntaxService = syntaxService;
        this.source = source;
        this.language = language;
    }

    @Override public void apply(IDocument document) {

        int startOffset;
        int endOffset;

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

        final Collection<ICompletionItem> completionItems = createItemsFromString(finalText);
        completion.setItems(completionItems);

        // re-parse to get the new AST to calculate nested completions
        P completedParseResult = null;

        try {
            final I input = unitService.inputUnit(source, finalText, language, null);
            completedParseResult = syntaxService.parse(input);
        } catch(ParseException e1) {
            e1.printStackTrace();
        }

        this.data =
            new CompletionData<>(document, textViewer, offset, completion, completionService, unitService, syntaxService,
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
                ui.setExitPosition(textViewer, data.cursorPosition, 0, data.cursorSequence);
                ui.enter();
            }
        } catch(BadLocationException e) {
            final String message =
                String.format("Cannot apply completion at offset %s, length %s", offset, data.text.length());
            logger.error(message, e);
        }
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
        return null;
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

    public Collection<ICompletionItem> createItemsFromString(String input) {

        StringBuffer sb = new StringBuffer();
        Collection<ICompletionItem> result = new LinkedList<ICompletionItem>();

        for(int i = 0; i < input.length(); i++) {

            if(input.charAt(i) == '[') {
                i++;
                if(input.charAt(i) == '[') { // might have found placeholder
                    i++;

                    while(i < input.length() && input.charAt(i) == '[') { // nested brackets
                        sb.append(input.charAt(i));
                        i++;
                    }

                    StringBuffer placeholderName = new StringBuffer();
                    while(i < input.length() && input.charAt(i) != ']') {
                        String charAti = String.valueOf(input.charAt(i));

                        if(!charAti.matches("[a-zA-Z_]")) { // not placeholder: abort
                            break;
                        }

                        placeholderName.append(charAti);
                        i++;
                    }

                    if(i >= input.length() || input.charAt(i) != ']') { // add two [[ and placeholder name to buffer
                        sb.append("[[" + placeholderName);
                        placeholderName.setLength(0);
                        continue;
                    }

                    i++;

                    final TextCompletionItem item = new TextCompletionItem(sb.toString());
                    result.add(item);

                    // TODO: calculate the list of completions for each placeholder
                    final PlaceholderCompletionItem placeholder =
                        new PlaceholderCompletionItem(placeholderName.toString(), placeholderName.toString());

                    result.add(placeholder);

                    sb.setLength(0);
                    continue;
                }
                sb.append(input.charAt(i - 1));
                sb.append(input.charAt(i));
            } else {
                sb.append(input.charAt(i));
            }
        }

        if(sb.length() != 0) {
            final TextCompletionItem item = new TextCompletionItem(sb.toString());
            result.add(item);
        }

        return result;
    }
}
