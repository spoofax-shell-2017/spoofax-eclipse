package org.metaborg.spoofax.eclipse.editor;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.ui.IEditorInput;
import org.metaborg.core.editor.IEditor;
import org.metaborg.core.outline.IOutline;
import org.metaborg.core.style.IRegionStyle;
import org.metaborg.spoofax.eclipse.util.Nullable;

public interface IEclipseEditor<P> extends IEditor {
    /**
     * @return Current input, or null if the editor has not been initialized yet, or if it has been disposed.
     */
    public abstract @Nullable IEditorInput input();

    /**
     * @return Current Eclipse resource, or null if the editor has not been initialized yet, or if it has been disposed.
     */
    public abstract @Nullable IResource eclipseResource();

    /**
     * @return Current document, or null if the editor has not been initialized yet, or if it has been disposed.
     */
    public abstract @Nullable IDocument document();

    /**
     * @return Source viewer, or null if the editor has not been initialized yet, or if it has been disposed.
     */
    public abstract @Nullable ISourceViewer sourceViewer();

    /**
     * @return Source viewer configuration, or null if the editor has not been initialized yet, or if it has been
     *         disposed.
     */
    public abstract @Nullable SourceViewerConfiguration configuration();


    /**
     * Sets the text styling, using given text and monitor for cancellation. Can be called from any thread.
     */
    public abstract void setStyle(Iterable<IRegionStyle<P>> style, String text, IProgressMonitor monitor);

    /**
     * Sets the outline information, using given monitor for cancellation. Can be called from any thread.
     */
    public abstract void setOutline(IOutline outline, IProgressMonitor monitor);


    /**
     * Opens the quick outline with previously set outline information.
     */
    public abstract void openQuickOutline();
}
