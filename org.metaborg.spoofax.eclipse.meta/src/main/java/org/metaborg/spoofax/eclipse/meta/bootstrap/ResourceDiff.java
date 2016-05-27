package org.metaborg.spoofax.eclipse.meta.bootstrap;

import org.apache.commons.vfs2.FileSystemException;
import org.metaborg.spoofax.eclipse.util.Nullable;

public class ResourceDiff {
    public final Object left;
    public final Object right;

    public final String message;
    public final @Nullable Object leftObj;
    public final @Nullable Object rightObj;


    public ResourceDiff(Object left, Object right, String message, @Nullable Object leftObj, @Nullable Object rightObj)
        throws FileSystemException {
        this.left = left;
        this.right = right;
        this.message = message;
        this.leftObj = leftObj;
        this.rightObj = rightObj;
    }

    @Override public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Resources ");
        sb.append(left.toString());
        sb.append(" and ");
        sb.append(right.toString());
        sb.append(" differ - ");
        sb.append(message);
        if(leftObj != null && rightObj != null) {
            sb.append(": ");
            sb.append(leftObj.toString());
            sb.append(" vs ");
            sb.append(rightObj.toString());
        }
        return sb.toString();
    }
}
