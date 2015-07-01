package org.metaborg.spoofax.eclipse.editor;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.metaborg.spoofax.core.source.ISourceLocation;
import org.metaborg.spoofax.core.tracing.Resolution;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.eclipse.util.EditorUtils;
import org.metaborg.spoofax.eclipse.util.RegionUtils;

import com.google.common.collect.Iterables;

final class SpoofaxHyperlink implements IHyperlink {
    private final IEclipseResourceService resourceService;

    private final Resolution resolution;
    private final FileObject editorResource;
    private final AbstractTextEditor editor;


    public SpoofaxHyperlink(IEclipseResourceService resourceService, Resolution resolution, FileObject editorResource,
        AbstractTextEditor editor) {
        this.resourceService = resourceService;

        this.resolution = resolution;
        this.editorResource = editorResource;
        this.editor = editor;
    }


    @Override public void open() {
        // GTODO: support multiple targets
        final ISourceLocation target = Iterables.get(resolution.targets, 0);
        final FileObject targetResource = target.resource();
        final int offset = target.region().startOffset();

        if(targetResource.getName().equals(editorResource.getName())) {
            EditorUtils.editorFocus(editor, offset);
        } else {
            final IResource eclipseResource = resourceService.unresolve(targetResource);
            if(eclipseResource != null && eclipseResource instanceof IFile) {
                final IFile file = (IFile) eclipseResource;
                EditorUtils.openEditor(file, offset);
            }
        }
    }

    @Override public String getTypeLabel() {
        return null;
    }

    @Override public String getHyperlinkText() {
        return null;
    }

    @Override public IRegion getHyperlinkRegion() {
        return RegionUtils.fromCore(resolution.highlight);
    }
}
