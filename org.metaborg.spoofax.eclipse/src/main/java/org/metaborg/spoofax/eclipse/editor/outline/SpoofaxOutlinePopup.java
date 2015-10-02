package org.metaborg.spoofax.eclipse.editor.outline;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.metaborg.core.outline.IOutline;
import org.metaborg.spoofax.eclipse.util.ui.FilteringInfoPopup;

public class SpoofaxOutlinePopup extends FilteringInfoPopup {
    private IOutline outline;


    public SpoofaxOutlinePopup(Shell parent) {
        super(parent, FilteringInfoPopup.INFOPOPUP_SHELLSTYLE, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
    }


    public void update(IOutline outline) {
        this.outline = outline;

        final TreeViewer treeViewer = getTreeViewer();
        if(treeViewer != null) {
            treeViewer.setInput(outline);
        }
    }


    @Override protected TreeViewer createTreeViewer(Composite parent, int style) {
        final TreeViewer treeViewer = new TreeViewer(parent, style);
        treeViewer.setContentProvider(new SpoofaxContentProvider());
        treeViewer.setLabelProvider(new SpoofaxLabelProvider());
        treeViewer.setAutoExpandLevel(TreeViewer.ALL_LEVELS);
        if(outline != null) {
            treeViewer.setInput(outline);
        }
        setMatcherString("", false);
        return treeViewer;
    }

    @Override protected String getId() {
        return SpoofaxOutlinePopup.class.toString();
    }

    @Override protected void handleElementSelected(Object selectedElement) {

    }

    @Override protected Point getDefaultSize() {
        final Point size = super.getDefaultSize();
        size.x = Math.max(size.x, 800);
        size.y = Math.max(size.y, 600);
        return size;
    }
}
