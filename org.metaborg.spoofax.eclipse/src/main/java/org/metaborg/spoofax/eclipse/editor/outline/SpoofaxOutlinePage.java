package org.metaborg.spoofax.eclipse.editor.outline;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;
import org.metaborg.core.outline.IOutline;
import org.metaborg.core.outline.IOutlineNode;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.spoofax.eclipse.util.EditorUtils;

public class SpoofaxOutlinePage extends ContentOutlinePage implements ISelectionChangedListener {
    private final AbstractTextEditor editor;

    private TreeViewer viewer;
    private Control control;


    public SpoofaxOutlinePage(AbstractTextEditor editor) {
        this.editor = editor;
    }


    public void update(IOutline outline) {
        if(viewer == null || control == null) {
            return;
        }

        control.setRedraw(false);
        viewer.setInput(outline);
        viewer.expandToLevel(outline.expandTo());
        control.setRedraw(true);
    }


    @Override public void createControl(Composite parent) {
        super.createControl(parent);

        viewer = getTreeViewer();
        control = viewer.getControl();

        viewer.setContentProvider(new SpoofaxContentProvider());
        viewer.setLabelProvider(new SpoofaxLabelProvider());

        addSelectionChangedListener(this);
    }

    @Override public void selectionChanged(SelectionChangedEvent event) {
        if(!viewer.equals(event.getSource())) {
            return;
        }

        final ISelection selection = event.getSelection();
        if(selection.isEmpty()) {
            return;
        }

        if(selection instanceof IStructuredSelection) {
            final IStructuredSelection structuredSelection = (IStructuredSelection) selection;
            final Object selected = structuredSelection.getFirstElement();
            final IOutlineNode node = OutlineUtils.node(selected);
            if(node == null) {
                return;
            }

            final ISourceRegion region = node.origin();
            if(region == null) {
                return;
            }
            EditorUtils.select(editor, region);
        }
    }
}
