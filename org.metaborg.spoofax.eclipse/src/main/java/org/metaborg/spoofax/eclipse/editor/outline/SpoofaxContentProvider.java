package org.metaborg.spoofax.eclipse.editor.outline;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.metaborg.core.outline.IOutline;
import org.metaborg.core.outline.IOutlineNode;

import com.google.common.collect.Iterables;

public class SpoofaxContentProvider implements ITreeContentProvider {
    @Override public void dispose() {
        // Do nothing
    }

    @Override public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        // Do nothing
    }


    @Override public Object[] getElements(Object input) {
        // Workaround for https://bugs.eclipse.org/9262, make sure that the root object does not equal the input.
        final IOutline outline = OutlineUtils.outline(input);
        if(outline != null) {
            return Iterables.toArray(outline.roots(), IOutlineNode.class);
        }

        final IOutlineNode node = OutlineUtils.node(input);
        if(node == null) {
            return new Object[0];
        }
        return new Object[] { node };
    }

    @Override public Object[] getChildren(Object element) {
        final IOutlineNode node = OutlineUtils.node(element);
        if(node == null) {
            return new Object[0];
        }
        return Iterables.toArray(node.nodes(), IOutlineNode.class);
    }

    @Override public Object getParent(Object element) {
        final IOutlineNode node = OutlineUtils.node(element);
        if(node == null) {
            return null;
        }
        return node.parent();
    }

    @Override public boolean hasChildren(Object element) {
        final IOutlineNode node = OutlineUtils.node(element);
        if(node == null) {
            return false;
        }
        return !Iterables.isEmpty(node.nodes());
    }
}
