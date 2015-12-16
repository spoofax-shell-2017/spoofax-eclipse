package org.metaborg.spoofax.eclipse.nature;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.metaborg.spoofax.eclipse.SpoofaxPlugin;
import org.metaborg.spoofax.eclipse.build.SpoofaxProjectBuilder;
import org.metaborg.spoofax.eclipse.util.BuilderUtils;
import org.metaborg.spoofax.eclipse.util.NatureUtils;
import org.metaborg.spoofax.eclipse.util.Nullable;

public class SpoofaxNature implements IProjectNature {
    public static final String id = SpoofaxPlugin.id + ".nature";

    private IProject project;


    @Override public void configure() throws CoreException {
        BuilderUtils.prepend(SpoofaxProjectBuilder.id, project, null);
    }

    @Override public void deconfigure() throws CoreException {
        BuilderUtils.removeFrom(SpoofaxProjectBuilder.id, project, null);
    }

    @Override public IProject getProject() {
        return project;
    }

    @Override public void setProject(IProject project) {
        this.project = project;
    }


    public static boolean exists(IProject project) throws CoreException {
        return NatureUtils.exists(id, project);
    }

    public static void add(IProject project, @Nullable IProgressMonitor monitor) throws CoreException {
        NatureUtils.addTo(id, project, monitor);
    }
}
