package org.metaborg.spoofax.eclipse.meta.nature;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.metaborg.spoofax.eclipse.build.SpoofaxProjectBuilder;
import org.metaborg.spoofax.eclipse.meta.SpoofaxMetaPlugin;
import org.metaborg.spoofax.eclipse.meta.build.SpoofaxPostJavaMetaProjectBuilder;
import org.metaborg.spoofax.eclipse.meta.build.SpoofaxPreJavaMetaProjectBuilder;
import org.metaborg.spoofax.eclipse.util.BuilderUtils;

public class SpoofaxMetaNature implements IProjectNature {
    public static final String id = SpoofaxMetaPlugin.id + ".nature";

    private IProject project;


    @Override public void configure() throws CoreException {
        BuilderUtils.addAfter(SpoofaxPreJavaMetaProjectBuilder.id, SpoofaxProjectBuilder.id, project,
            IncrementalProjectBuilder.FULL_BUILD, IncrementalProjectBuilder.CLEAN_BUILD);
        BuilderUtils.addAfter(SpoofaxPostJavaMetaProjectBuilder.id, "org.eclipse.jdt.core.javabuilder", project,
            IncrementalProjectBuilder.FULL_BUILD, IncrementalProjectBuilder.CLEAN_BUILD);
    }

    @Override public void deconfigure() throws CoreException {
        BuilderUtils.removeFrom(SpoofaxPreJavaMetaProjectBuilder.id, project);
        BuilderUtils.removeFrom(SpoofaxPostJavaMetaProjectBuilder.id, project);
    }

    @Override public IProject getProject() {
        return project;
    }

    @Override public void setProject(IProject project) {
        this.project = project;
    }
}
