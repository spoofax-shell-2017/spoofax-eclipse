package org.metaborg.spoofax.eclipse.editor.completion;

import java.util.Collection;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.metaborg.core.completion.ICompletion;
import org.metaborg.core.completion.ICompletionItem;
import org.metaborg.core.completion.ICursorCompletionItem;
import org.metaborg.core.completion.IPlaceholderCompletionItem;
import org.metaborg.core.completion.ITextCompletionItem;
import org.metaborg.spoofax.core.completion.PlaceholderCompletionItem;
import org.metaborg.spoofax.core.completion.TextCompletionItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class SpoofaxCompletionProposal implements ICompletionProposal {
    private static class CompletionData {
        public final String text;
        public final Multimap<String, LinkedPosition> placeholders;
        public final int cursorPosition;
        public final int cursorSequence;
        public static int sequence = 0;



        public CompletionData(IDocument document, int offset, ICompletion completion) {
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
                    final LinkedPosition position =
                        new LinkedPosition(document, textOffset, textLength, getNextSequence(placeholdersCompletion));
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
                        
                        while(i < input.length() && input.charAt(i) == '[') // nested brackets
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
    private final int offset;
    private final ICompletion completion;

    private CompletionData data;


    public SpoofaxCompletionProposal(ITextViewer textViewer, int offset, ICompletion completion) {
        this.textViewer = textViewer;
        this.offset = offset;
        this.completion = completion;
    }


    @Override public void apply(IDocument document) {
        this.data = new CompletionData(document, offset, completion);

        try {
            
            document.replace(0, document.getLength(), data.text);

            if(!data.placeholders.isEmpty()) {
                final LinkedModeModel model = new LinkedModeModel();
                for(Collection<LinkedPosition> positions : data.placeholders.asMap().values()) {
                    final LinkedPositionGroup group = new LinkedPositionGroup();
                    for(LinkedPosition position : positions) {
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
}
