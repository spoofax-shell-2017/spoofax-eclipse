package org.metaborg.spoofax.eclipse.meta.bootstrap;

import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileSystemException;
import org.metaborg.spoofax.eclipse.util.Nullable;

public class ResourceDiff {
    public final FileName left;
    public final FileName right;

    public final String leftPath;
    public final String rightPath;

    public final String message;
    public final @Nullable Object leftObj;
    public final @Nullable Object rightObj;


    public ResourceDiff(@Nullable FileName root, FileName left, FileName right, String message,
        @Nullable Object leftObj, @Nullable Object rightObj) throws FileSystemException {
        this.left = left;
        this.right = right;
        this.message = message;
        this.leftObj = leftObj;
        this.rightObj = rightObj;

        if(root != null) {
            leftPath = root.getRelativeName(left);
            rightPath = root.getRelativeName(right);
        } else {
            leftPath = left.getURI();
            rightPath = right.getURI();
        }
    }

    public ResourceDiff(FileName left, FileName right, String message, @Nullable Object leftObj,
        @Nullable Object rightObj) throws FileSystemException {
        this(null, left, right, message, leftObj, rightObj);
    }

    public ResourceDiff(FileName root, FileName left, FileName right, String message) throws FileSystemException {
        this(root, left, right, message, null, null);
    }

    public ResourceDiff(FileName left, FileName right, String message) throws FileSystemException {
        this(null, left, right, message, null, null);
    }


    @Override public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Resources ");
        sb.append(leftPath);
        sb.append(" and ");
        sb.append(rightPath);
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
