package org.metaborg.spoofax.eclipse.editor.outline;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;
import org.metaborg.core.outline.IOutline;

public class SpoofaxOutlinePage extends ContentOutlinePage {
    private TreeViewer viewer;
    private Control control;


    @Override public void createControl(Composite parent) {
        super.createControl(parent);

        viewer = getTreeViewer();
        control = viewer.getControl();

        viewer.setContentProvider(new SpoofaxContentProvider());
        viewer.setLabelProvider(new SpoofaxLabelProvider());
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
}
