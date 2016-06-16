package org.metaborg.spoofax.eclipse.meta.bootstrap;

import java.io.File;
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


    public ResourceComparer(IResourceService resourceService) {
        this.resourceService = resourceService;
    }


    public Collection<ResourceDiff> compare(FileObject left, FileObject right)
        throws IOException, InterruptedException {
        final Collection<ResourceDiff> diffs = Lists.newArrayList();
        compare(left, left, right, right, diffs);
        return diffs;
    }


    private void compare(FileObject leftRoot, FileObject left, FileObject rightRoot, FileObject right,
        Collection<ResourceDiff> diffs) throws IOException, InterruptedException {
        if(!left.exists()) {
            throw new IOException("Left resource " + left + " does not exist");
        }
        if(!right.exists()) {
            throw new IOException("Right resource " + right + " does not exist");
        }

        final FileType leftType = left.getType();
        final FileType rightType = right.getType();
        if(!leftType.equals(rightType)) {
            diffs.add(diff(leftRoot, left, rightRoot, right, "different types", leftType, rightType));
            return;
        }

        switch(leftType) {
            case FILE:
                final boolean leftIsArchive = isArchive(left);
                final boolean rightIsArchive = isArchive(right);
                if(leftIsArchive ^ rightIsArchive) {
                    diffs.add(diff(leftRoot, left, rightRoot, right, "different types",
                        leftIsArchive ? "archive" : "non-archive", rightIsArchive ? "archive" : "non-archive"));
                    return;
                }

                if(leftIsArchive) {
                    final FileObject leftArchive = toArchiveResource(left);
                    final FileObject rightArchive = toArchiveResource(right);
                    // HACK: pass archives as roots to shorten paths
                    compareDirectories(leftArchive, leftArchive, rightArchive, rightArchive, diffs);
                } else {
                    compareFiles(leftRoot, left, rightRoot, right, diffs);
                }
                break;
            case FOLDER:
                compareDirectories(leftRoot, left, rightRoot, right, diffs);
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


    private void compareDirectories(FileObject leftRoot, FileObject left, FileObject rightRoot, FileObject right,
        Collection<ResourceDiff> diffs) throws IOException, InterruptedException {
        final Set<String> leftChildren = children(left);
        final Set<String> rightChildren = children(right);

        final Set<String> onlyInLeft = Sets.difference(leftChildren, rightChildren);
        for(String name : onlyInLeft) {
            diffs.add(diff(leftRoot, left, rightRoot, right, "only in left: " + name));
        }

        final Set<String> onlyInRight = Sets.difference(rightChildren, leftChildren);
        for(String name : onlyInRight) {
            diffs.add(diff(leftRoot, left, rightRoot, right, "only in right: " + name));
        }

        final Set<String> both = Sets.intersection(leftChildren, rightChildren);
        for(String name : both) {
            final FileObject nextLeft = left.resolveFile(name);
            final FileObject nextRight = right.resolveFile(name);
            compare(leftRoot, nextLeft, rightRoot, nextRight, diffs);
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


    private void compareFiles(FileObject leftRoot, FileObject left, FileObject rightRoot, FileObject right,
        Collection<ResourceDiff> diffs) throws IOException, InterruptedException {
        final String leftName = left.getName().getBaseName();
        final String rightName = right.getName().getBaseName();
        if(!leftName.equals(rightName)) {
            diffs.add(diff(leftRoot, left, rightRoot, right, "different names", leftName, rightName));
            return;
        }

        final String extension = left.getName().getExtension();
        switch(extension) {
            case "class":
                compareClassFiles(leftRoot, left, rightRoot, right, diffs);
                break;
            default:
                compareGenericFiles(leftRoot, left, rightRoot, right, diffs);
                break;
        }
    }

    private void compareGenericFiles(FileObject leftRoot, FileObject left, FileObject rightRoot, FileObject right,
        Collection<ResourceDiff> diffs) throws IOException {
        final FileContent leftContent = left.getContent();
        final FileContent rightContent = right.getContent();

        final long leftSize = leftContent.getSize();
        final long rightSize = rightContent.getSize();
        if(leftSize != rightSize) {
            diffs.add(diff(leftRoot, left, rightRoot, right, "different sizes", leftSize, rightSize));
            return;
        }

        final InputStream leftStream = leftContent.getInputStream();
        final InputStream rightStream = rightContent.getInputStream();
        if(!IOUtils.contentEquals(leftStream, rightStream)) {
            diffs.add(diff(leftRoot, left, rightRoot, right, "different content"));
        }
    }

    private void compareClassFiles(FileObject leftRoot, FileObject left, FileObject rightRoot, FileObject right,
        Collection<ResourceDiff> diffs) throws IOException, InterruptedException {
        final FileContent leftContent = left.getContent();
        final FileContent rightContent = right.getContent();

        final long leftSize = leftContent.getSize();
        final long rightSize = rightContent.getSize();
        if(leftSize == rightSize) {
            return;
        }

        final InputStream leftStream = leftContent.getInputStream();
        final InputStream rightStream = rightContent.getInputStream();
        if(IOUtils.contentEquals(leftStream, rightStream)) {
            return;
        }

        /*
         * Some Java compilers are not completely deterministic, they sometimes generate less, more, or different bytes
         * for the exact same Java source files. To make sure that those class files are not marked as different, we
         * first disassemble them with the javap tool, and compare the disassembled output.
         */
        final String leftDisassembly = disassembleClassFile(left);
        final String rightDisassembly = disassembleClassFile(right);
        if(!leftDisassembly.equals(rightDisassembly)) {
            diffs.add(diff(leftRoot, left, rightRoot, right, "different content (after disassembly)"));
        }
    }

    private String disassembleClassFile(FileObject classFile) throws InterruptedException, IOException {
        final File file = resourceService.localFile(classFile);

        // Run javap with -c to disassemble code within methods, and -p to disassemble all private members.
        final ProcessBuilder processBuilder = new ProcessBuilder("javap -c -p " + file);
        final Process process = processBuilder.start();
        final String disassembly = IOUtils.toString(process.getInputStream());
        process.waitFor();

        return disassembly;
    }


    private ResourceDiff diff(@Nullable FileObject leftRoot, FileObject left, @Nullable FileObject rightRoot,
        FileObject right, String message) throws FileSystemException {
        return diff(leftRoot, left, rightRoot, right, message, null, null);
    }

    private ResourceDiff diff(@Nullable FileObject leftRoot, FileObject left, @Nullable FileObject rightRoot,
        FileObject right, String message, @Nullable Object leftObj, @Nullable Object rightObj)
        throws FileSystemException {
        final String leftStr;
        if(leftRoot != null) {
            leftStr = leftRoot.getName().getRelativeName(left.getName());
        } else {
            leftStr = left.getName().toString();
        }
        final String rightStr;
        if(rightRoot != null) {
            rightStr = rightRoot.getName().getRelativeName(right.getName());
        } else {
            rightStr = right.getName().toString();
        }
        return new ResourceDiff(leftStr, rightStr, message, leftObj, rightObj);
    }
}
