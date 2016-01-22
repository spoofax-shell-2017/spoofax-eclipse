package org.metaborg.spoofax.eclipse.editor.outline;

import org.metaborg.core.outline.IOutline;
import org.metaborg.core.outline.IOutlineNode;
import org.metaborg.spoofax.eclipse.util.Nullable;

public class OutlineUtils {
    public static
    @Nullable IOutline outline(Object obj) {
        if(obj instanceof IOutline) {
            final IOutline outline = (IOutline) obj;
            return outline;
        }
        return null;
    }

    public static
    @Nullable IOutlineNode node(Object obj) {
        if(obj instanceof IOutlineNode) {
            final IOutlineNode node = (IOutlineNode) obj;
            return node;
        }
        return null;
    }
}
