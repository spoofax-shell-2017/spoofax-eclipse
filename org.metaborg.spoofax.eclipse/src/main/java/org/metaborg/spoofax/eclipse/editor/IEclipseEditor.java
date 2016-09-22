package org.metaborg.spoofax.eclipse.editor;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;
import org.metaborg.core.editor.IEditor;
import org.metaborg.core.outline.IOutline;
import org.metaborg.core.style.IRegionStyle;
import org.metaborg.spoofax.eclipse.util.Nullable;

public interface IEclipseEditor<F> extends IEditor, ITextEditor {
    /**
     * @return Current input, or null if the editor has not been initialized yet, or if it has been disposed.
     */
    @Nullable IEditorInput input();

    /**
     * @return Current Eclipse resource, or null if the editor has not been initialized yet, or if it has been disposed.
     */
    @Nullable IResource eclipseResource();

    /**
     * @return Current document, or null if the editor has not been initialized yet, or if it has been disposed.
     */
    @Nullable IDocument document();

    /**
     * @return Source viewer, or null if the editor has not been initialized yet, or if it has been disposed.
     */
    @Nullable ISourceViewer sourceViewer();

    /**
     * @return Source viewer configuration, or null if the editor has not been initialized yet, or if it has been
     *         disposed.
     */
    @Nullable SourceViewerConfiguration configuration();
    
    
    /**
     * @return True if the editor is updating. False otherwise.
     */
    boolean editorIsUpdating();


    /**
     * @return Selection provider.
     */
    ISelectionProvider selectionProvider();

    /**
     * @return Text operation target.
     */
    ITextOperationTarget textOperationTarget();


    /**
     * Sets the text styling, using given text and monitor for cancellation. Can be called from any thread.
     */
    void setStyle(Iterable<IRegionStyle<F>> style, String text, IProgressMonitor monitor);

    /**
     * Sets the outline information, using given monitor for cancellation. Can be called from any thread.
     */
    void setOutline(IOutline outline, IProgressMonitor monitor);


    /**
     * Opens the quick outline with previously set outline information.
     */
    void openQuickOutline();
}
