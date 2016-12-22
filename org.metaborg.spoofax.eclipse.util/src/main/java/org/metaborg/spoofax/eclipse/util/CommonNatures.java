package org.metaborg.spoofax.eclipse.util;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class CommonNatures {
    public static final String javaNatureId = "org.eclipse.jdt.core.javanature";
    public static final String mavenNatureId = "org.eclipse.m2e.core.maven2Nature";
    public static final String pdePluginNatureId = "org.eclipse.pde.PluginNature";
    public static final String pdeFeatureNatureId = "org.eclipse.pde.FeatureNature";
    public static final String pdeSiteNatureId = "org.eclipse.pde.UpdateSiteNature";


    public static void addJavaNature(IProject project, @Nullable IProgressMonitor monitor) throws CoreException {
        NatureUtils.addTo(javaNatureId, project, monitor);
    }

    public static void addMavenNature(IProject project, @Nullable IProgressMonitor monitor) throws CoreException {
        NatureUtils.addTo(mavenNatureId, project, monitor);
    }

    public static void addPdePluginNature(IProject project, @Nullable IProgressMonitor monitor) throws CoreException {
        NatureUtils.addTo(pdePluginNatureId, project, monitor);
    }

    public static void addPdeFeatureNature(IProject project, @Nullable IProgressMonitor monitor) throws CoreException {
        NatureUtils.addTo(pdeFeatureNatureId, project, monitor);
    }

    public static void addPdeSiteNature(IProject project, @Nullable IProgressMonitor monitor) throws CoreException {
        NatureUtils.addTo(pdeSiteNatureId, project, monitor);
    }
}
