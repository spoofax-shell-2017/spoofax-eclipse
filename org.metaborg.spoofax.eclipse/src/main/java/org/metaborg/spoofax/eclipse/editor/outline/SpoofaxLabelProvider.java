package org.metaborg.spoofax.eclipse.editor.outline;

import java.util.Map;

import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.metaborg.core.outline.IOutlineNode;
import org.metaborg.spoofax.eclipse.util.Nullable;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.Maps;

public class SpoofaxLabelProvider extends LabelProvider {
    private static final ILogger logger = LoggerUtils.logger(SpoofaxLabelProvider.class);

    private final Map<FileName, Image> icons = Maps.newHashMap();


    @Override public String getText(Object element) {
        final IOutlineNode node = OutlineUtils.node(element);
        if(node == null) {
            return null;
        }
        return node.label();
    }

    @Override public @Nullable Image getImage(Object element) {
        final IOutlineNode node = OutlineUtils.node(element);
        if(node == null) {
            return null;
        }

        final FileObject iconFile = node.icon();
        try {
            if(iconFile == null) {
                return null;
            }
            final FileName iconFileName = iconFile.getName();

            Image icon = icons.get(iconFileName);
            if(icon == null) {
                icon = new Image(Display.getDefault(), iconFile.getContent().getInputStream());
                icons.put(iconFileName, icon);
            }
            return icon;
        } catch(FileSystemException e) {
            logger.error("Cannot create icon {} for outline node {}", e, iconFile, node);
            return null;
        }
    }
}
