package org.metaborg.spoofax.eclipse.editor;

import org.metaborg.core.editor.IEditorRegistry;
import org.metaborg.spoofax.eclipse.util.Nullable;


public interface IEclipseEditorRegistry<F> extends IEditorRegistry {
    /**
     * @return All open Spoofax editors.
     */
    Iterable<IEclipseEditor<F>> openEclipseEditors();

    /**
     * @return Current active Spoofax editor, or null if none.
     */
    @Nullable IEclipseEditor<F> currentEditor();

    /**
     * Returns the previously active Spoofax editor when a Spoofax editor was active, followed by activation of a
     * non-editor part, like the package explorer or outline. When a non-Spoofax editor is activated, such as a JDT
     * editor, this returns null again.
     * 
     * Returns the same value as {@link #currentEditor()} if a Spoofax editor is currently active.
     * 
     * @return Previously active Spoofax editor, or null if none.
     */
    @Nullable IEclipseEditor<F> previousEditor();
}