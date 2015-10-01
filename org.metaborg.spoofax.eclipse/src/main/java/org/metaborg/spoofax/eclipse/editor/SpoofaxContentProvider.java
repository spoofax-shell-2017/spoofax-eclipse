package org.metaborg.spoofax.eclipse.editor;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.metaborg.core.outline.IOutline;
import org.metaborg.core.outline.IOutlineNode;
import org.metaborg.spoofax.eclipse.util.Nullable;

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
        final IOutline outline = outline(input);
        if(outline != null) {
            return new Object[] { outline.root() };
        }

        final IOutlineNode node = node(input);
        if(node == null) {
            return new Object[0];
        }
        return new Object[] { node };
    }

    @Override public Object[] getChildren(Object element) {
        final IOutlineNode node = node(element);
        if(node == null) {
            return new Object[0];
        }
        return Iterables.toArray(node.nodes(), IOutlineNode.class);
    }

    @Override public Object getParent(Object element) {
        final IOutlineNode node = node(element);
        if(node == null) {
            return null;
        }
        return node.parent();
    }

    @Override public boolean hasChildren(Object element) {
        final IOutlineNode node = node(element);
        if(node == null) {
            return false;
        }
        return !Iterables.isEmpty(node.nodes());
    }


    private final @Nullable IOutline outline(Object obj) {
        if(obj instanceof IOutline) {
            final IOutline outline = (IOutline) obj;
            return outline;
        }
        return null;
    }

    private final @Nullable IOutlineNode node(Object obj) {
        if(obj instanceof IOutlineNode) {
            final IOutlineNode node = (IOutlineNode) obj;
            return node;
        }
        return null;
    }
}
