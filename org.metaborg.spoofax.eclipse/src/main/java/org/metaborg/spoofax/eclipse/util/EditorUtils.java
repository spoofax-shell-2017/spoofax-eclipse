package org.metaborg.spoofax.eclipse.util;

import java.net.URI;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IRegion;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.MultiPartInitException;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.Iterables;

public class EditorUtils {
    private static final ILogger logger = LoggerUtils.logger(EditorUtils.class);


    public static void open(IFile file) {
        open(file, -1);
    }

    public static void open(final IFile file, final int offset) {
        final Display display = Display.getDefault();
        display.asyncExec(new Runnable() {
            @Override public void run() {
                final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                try {
                    final IEditorPart editorPart = IDE.openEditor(page, file);
                    if(offset >= 0) {
                        if(editorPart instanceof ITextEditor) {
                            final ITextEditor editor = (ITextEditor) editorPart;
                            selectAndFocus(editor, offset);
                        }
                    }
                } catch(PartInitException e) {
                    logger.error("Cannot open editor", e);
                }
            }
        });

    }

    public static void open(final Iterable<IFile> files) {
        open(files, -1);
    }

    public static void open(final Iterable<IFile> files, final int conformationThreshold) {
        // Run in the UI thread because we need to get the active workbench window and page.
        final int count = Iterables.size(files);
        final Display display = Display.getDefault();
        display.asyncExec(new Runnable() {
            @Override public void run() {
                if(conformationThreshold > 0 && count > conformationThreshold &&
                        !MessageDialog.openConfirm(display.getActiveShell(), "Opening multiple editors", "About to open "+count+" editors. Are you sure?")) {
                    return;
                }
                final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                try {
                    IDE.openEditors(page, Iterables.toArray(files, IFile.class));
                } catch(MultiPartInitException e) {
                    logger.error("Cannot open editor", e);
                }
            }
        });
    }

    public static void open(URI uri) {
        open(uri, -1);
    }

    public static void open(final URI uri, final int offset) {
        // Run in the UI thread because we need to get the active workbench window and page.
        final Display display = Display.getDefault();
        display.asyncExec(new Runnable() {
            @Override public void run() {
                final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                try {
                    final IFileStore fileStore = EFS.getStore(uri);
                    final IEditorPart editorPart = IDE.openEditorOnFileStore(page, fileStore);
                    if(offset >= 0) {
                        if(editorPart instanceof ITextEditor) {
                            final ITextEditor editor = (ITextEditor) editorPart;
                            selectAndFocus(editor, offset);
                        }
                    }
                } catch(CoreException e) {
                    logger.error("Cannot open editor", e);
                }
            }
        });
    }


    public static void select(ITextEditor editor, int offset) {
        select(editor, offset, 0);
    }

    public static void select(ITextEditor editor, int offset, int length) {
        editor.selectAndReveal(offset, length);
    }

    public static void select(ITextEditor editor, ISourceRegion region) {
        final IRegion eclipseRegion = RegionUtils.fromCore(region);
        select(editor, eclipseRegion.getOffset(), eclipseRegion.getLength());
    }


    public static void focus(ITextEditor editor) {
        editor.setFocus();
    }


    public static void selectAndFocus(ITextEditor editor, int offset) {
        select(editor, offset);
        focus(editor);
    }

    public static void selectAndFocus(ITextEditor editor, int offset, int length) {
        select(editor, offset, length);
        focus(editor);
    }

    public static void selectAndFocus(ITextEditor editor, ISourceRegion region) {
        select(editor, region);
        focus(editor);
    }
}
