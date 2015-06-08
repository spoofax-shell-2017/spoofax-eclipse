package org.metaborg.spoofax.eclipse.util;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditorUtils {
    private static final Logger logger = LoggerFactory.getLogger(EditorUtils.class);


    public static void openEditor(final IFile file) {
        openEditor(file, -1);
    }

    public static void openEditor(final IFile file, final int offset) {
        // Run in the UI thread because we need to get the active workbench window and page.
        final Display display = Display.getDefault();
        display.asyncExec(new Runnable() {
            @Override public void run() {
                final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                try {
                    final IEditorPart editorPart = IDE.openEditor(page, file);
                    if(offset >= 0) {
                        if(editorPart instanceof AbstractTextEditor) {
                            final AbstractTextEditor editor = (AbstractTextEditor) editorPart;
                            editorFocus(editor, offset);
                        }
                    }
                } catch(PartInitException e) {
                    logger.error("Cannot open editor", e);
                }
            }
        });
    }

    public static void editorFocus(AbstractTextEditor editor, int offset) {
        editor.selectAndReveal(offset, 0);
        editor.setFocus();
    }
}
