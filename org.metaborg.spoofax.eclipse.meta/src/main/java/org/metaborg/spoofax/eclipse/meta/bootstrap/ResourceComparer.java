package org.metaborg.spoofax.eclipse.meta.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.metaborg.core.resource.IResourceService;
import org.metaborg.spoofax.eclipse.util.Nullable;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class ResourceComparer {
    private final IResourceService resourceService;
    private final @Nullable FileName root;


    public ResourceComparer(IResourceService resourceService, @Nullable FileName root) {
        this.resourceService = resourceService;
        this.root = root;
    }


    public Collection<ResourceDiff> compare(FileObject left, FileObject right) throws IOException {
        final Collection<ResourceDiff> diffs = Lists.newArrayList();
        compare(left, right, diffs);
        return diffs;
    }


    private void compare(FileObject left, FileObject right, Collection<ResourceDiff> diffs) throws IOException {
        if(!left.exists()) {
            throw new IOException("Left resource " + left + " does not exist");
        }
        if(!right.exists()) {
            throw new IOException("Right resource " + right + " does not exist");
        }

        final FileType leftType = left.getType();
        final FileType rightType = right.getType();
        if(!leftType.equals(rightType)) {
            diffs.add(diff(left, right, "different types", leftType, rightType));
            return;
        }

        switch(leftType) {
            case FILE:
                final boolean leftIsArchive = isArchive(left);
                final boolean rightIsArchive = isArchive(right);
                if(leftIsArchive ^ rightIsArchive) {
                    diffs.add(diff(left, right, "different types", leftIsArchive ? "archive" : "non-archive",
                        rightIsArchive ? "archive" : "non-archive"));
                    return;
                }

                if(leftIsArchive) {
                    final FileObject leftArchive = toArchiveResource(left);
                    final FileObject rightArchive = toArchiveResource(right);
                    compareDirectories(leftArchive, rightArchive, diffs);
                } else {
                    compareFiles(left, right, diffs);
                }
                break;
            case FOLDER:
                compareDirectories(left, right, diffs);
                break;
            default:
                throw new IOException("Unhandled resource type: " + leftType.toString());
        }
    }

    private boolean isArchive(FileObject resource) {
        final FileName name = resource.getName();
        final String extension = name.getExtension();
        switch(extension) {
            // TODO: tar.gz and tar.bz2
            case "spoofax-language":
            case "zip":
            case "jar":
            case "gz":
            case "bz2":
                return true;
            default:
                return false;
        }
    }

    private FileObject toArchiveResource(FileObject resource) {
        final FileName name = resource.getName();
        final String extension = name.getExtension();
        switch(extension) {
            // TODO: tar.gz and tar.bz2
            case "spoofax-language":
            case "zip":
                return resourceService.resolve("zip:" + name.getURI() + "!/");
            case "jar":
                return resourceService.resolve("jar:" + name.getURI() + "!/");
            case "gz":
                return resourceService.resolve("gz:" + name.getURI() + "!/");
            case "bz2":
                return resourceService.resolve("bz2:" + name.getURI() + "!/");
            default:
                return resource;
        }
    }


    private void compareDirectories(FileObject left, FileObject right, Collection<ResourceDiff> diffs)
        throws IOException {
        final Set<String> leftChildren = children(left);
        final Set<String> rightChildren = children(right);

        final Set<String> onlyInLeft = Sets.difference(leftChildren, rightChildren);
        for(String name : onlyInLeft) {
            diffs.add(diff(left, right, "only in left: " + name));
        }

        final Set<String> onlyInRight = Sets.difference(rightChildren, leftChildren);
        for(String name : onlyInRight) {
            diffs.add(diff(left, right, "only in right: " + name));
        }

        final Set<String> both = Sets.intersection(leftChildren, rightChildren);
        for(String name : both) {
            final FileObject nextLeft = left.resolveFile(name);
            final FileObject nextRight = right.resolveFile(name);
            compare(nextLeft, nextRight, diffs);
        }
    }

    private Set<String> children(FileObject directory) throws FileSystemException {
        final FileName name = directory.getName();
        final Set<String> children = Sets.newTreeSet();
        for(FileObject child : directory.getChildren()) {
            final String rel = name.getRelativeName(child.getName());
            children.add(rel);
        }
        return children;
    }


    private void compareFiles(FileObject left, FileObject right, Collection<ResourceDiff> diffs) throws IOException {
        final FileContent leftContent = left.getContent();
        final FileContent rightContent = right.getContent();

        final long leftSize = leftContent.getSize();
        final long rightSize = rightContent.getSize();
        if(leftSize != rightSize) {
            diffs.add(diff(left, right, "different sizes", leftSize, rightSize));
            return;
        }

        final InputStream leftStream = leftContent.getInputStream();
        final InputStream rightStream = rightContent.getInputStream();
        if(!IOUtils.contentEquals(leftStream, rightStream)) {
            diffs.add(diff(left, right, "different content"));
        }
    }


    private ResourceDiff diff(FileObject left, FileObject right, String message) throws FileSystemException {
        return diff(left.getName(), right.getName(), message);
    }

    private ResourceDiff diff(FileName left, FileName right, String message) throws FileSystemException {
        return new ResourceDiff(root, left, right, message);
    }

    private ResourceDiff diff(FileObject left, FileObject right, String message, Object leftObj, Object rightObj)
        throws FileSystemException {
        return diff(left.getName(), right.getName(), message, leftObj, rightObj);
    }

    private ResourceDiff diff(FileName left, FileName right, String message, Object leftObj, Object rightObj)
        throws FileSystemException {
        return new ResourceDiff(root, left, right, message, leftObj, rightObj);
    }
}
