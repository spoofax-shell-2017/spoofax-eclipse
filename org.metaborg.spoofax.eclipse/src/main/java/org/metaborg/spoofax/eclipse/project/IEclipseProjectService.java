package org.metaborg.spoofax.eclipse.project;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.project.IProject;
import org.metaborg.core.project.IProjectService;
import org.metaborg.spoofax.eclipse.util.Nullable;

public interface IEclipseProjectService extends IProjectService {
    @Nullable EclipseProject get(FileObject resource);

    @Nullable EclipseProject get(IProject project);
}
