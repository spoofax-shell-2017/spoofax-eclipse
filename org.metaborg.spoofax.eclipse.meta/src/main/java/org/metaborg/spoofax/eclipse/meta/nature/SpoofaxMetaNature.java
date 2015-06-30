package org.metaborg.spoofax.eclipse.meta.nature;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.metaborg.spoofax.eclipse.build.SpoofaxProjectBuilder;
import org.metaborg.spoofax.eclipse.meta.SpoofaxMetaPlugin;
import org.metaborg.spoofax.eclipse.meta.build.GenerateSourcesBuilder;
import org.metaborg.spoofax.eclipse.meta.build.PostJavaBuilder;
import org.metaborg.spoofax.eclipse.meta.build.PreJavaBuilder;
import org.metaborg.spoofax.eclipse.nature.SpoofaxNature;
import org.metaborg.spoofax.eclipse.util.BuilderUtils;
import org.metaborg.spoofax.eclipse.util.NatureUtils;

public class SpoofaxMetaNature implements IProjectNature {
    public static final String id = SpoofaxMetaPlugin.id + ".nature";

    private static final String javaNatureId = "org.eclipse.jdt.core.javanature";
    private static final String javaBuilderId = "org.eclipse.jdt.core.javabuilder";
    private static final String mavenNatureId = "org.eclipse.m2e.core.maven2Nature";
    private static final String mavenBuilderId = "org.eclipse.m2e.core.maven2Builder";

    private IProject project;


    @Override public void configure() throws CoreException {
        BuilderUtils.append(GenerateSourcesBuilder.id, project, IncrementalProjectBuilder.FULL_BUILD,
            IncrementalProjectBuilder.CLEAN_BUILD);
        BuilderUtils.append(PreJavaBuilder.id, project, IncrementalProjectBuilder.FULL_BUILD,
            IncrementalProjectBuilder.CLEAN_BUILD);
        BuilderUtils.append(PostJavaBuilder.id, project, IncrementalProjectBuilder.FULL_BUILD,
            IncrementalProjectBuilder.CLEAN_BUILD);
    }

    @Override public void deconfigure() throws CoreException {
        BuilderUtils.removeFrom(GenerateSourcesBuilder.id, project);
        BuilderUtils.removeFrom(PreJavaBuilder.id, project);
        BuilderUtils.removeFrom(PostJavaBuilder.id, project);
    }

    @Override public IProject getProject() {
        return project;
    }

    @Override public void setProject(IProject project) {
        this.project = project;
    }


    public static void add(IProject project) throws CoreException {
        addDependencyNatures(project);
        NatureUtils.addTo(id, project);
        sortBuilders(project);
    }

    private static void addDependencyNatures(IProject project) throws CoreException {
        NatureUtils.addTo(mavenNatureId, project);
        NatureUtils.addTo(javaNatureId, project);
        NatureUtils.addTo(SpoofaxNature.id, project);
    }

    private static void sortBuilders(IProject project) throws CoreException {
        final String[] buildOrder =
            new String[] { mavenBuilderId, GenerateSourcesBuilder.id, SpoofaxProjectBuilder.id, PreJavaBuilder.id,
                javaBuilderId, PostJavaBuilder.id };
        BuilderUtils.sort(project, buildOrder);
    }
}
