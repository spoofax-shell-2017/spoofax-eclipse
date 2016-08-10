package org.metaborg.spoofax.eclipse.util;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class CommonBuilders {
    public static final String javaBuilderId = "org.eclipse.jdt.core.javabuilder";
    public static final String mavenBuilderId = "org.eclipse.m2e.core.maven2Builder";


    public static void appendJavaBuilder(IProject project, @Nullable IProgressMonitor monitor, int... triggers)
        throws CoreException {
        BuilderUtils.append(javaBuilderId, project, monitor, triggers);
    }

    public static void appendMavenBuilder(IProject project, @Nullable IProgressMonitor monitor, int... triggers)
        throws CoreException {
        BuilderUtils.append(mavenBuilderId, project, monitor, triggers);
    }
}
