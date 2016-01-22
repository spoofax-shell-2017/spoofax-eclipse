package org.metaborg.spoofax.eclipse.language;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.language.*;
import org.metaborg.core.language.dialect.IDialectProcessor;
import org.metaborg.core.project.IProjectService;
import org.metaborg.core.project.settings.ILegacyProjectSettings;
import org.metaborg.core.project.settings.ILegacyProjectSettingsService;
import org.metaborg.core.resource.ResourceChange;
import org.metaborg.core.resource.ResourceChangeKind;
import org.metaborg.core.resource.ResourceUtils;
import org.metaborg.spoofax.eclipse.job.GlobalSchedulingRules;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.osgi.framework.Bundle;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

/**
 * Discovers all languages in plugins and workspace projects, and discovers languages when a project is opened.
 */
public class EclipseLanguageLoader implements IResourceChangeListener {
    private static final ILogger logger = LoggerUtils.logger(EclipseLanguageLoader.class);


    private final IEclipseResourceService resourceService;
    private final ILanguageService languageService;
    private final INewLanguageDiscoveryService languageDiscoveryService;
    private final IDialectProcessor dialectProcessor;
    private final IProjectService projectService;
    private final ILegacyProjectSettingsService projectSettingsService;

    private final GlobalSchedulingRules globalRules;
    private final IWorkspaceRoot workspaceRoot;


    @Inject public EclipseLanguageLoader(IEclipseResourceService resourceService, ILanguageService languageService,
                                         INewLanguageDiscoveryService languageDiscoveryService, IDialectProcessor dialectProcessor,
                                         IProjectService projectService, ILegacyProjectSettingsService projectSettingsService,
                                         GlobalSchedulingRules globalRules) {
        this.resourceService = resourceService;
        this.languageService = languageService;
        this.languageDiscoveryService = languageDiscoveryService;
        this.dialectProcessor = dialectProcessor;
        this.projectService = projectService;
        this.projectSettingsService = projectSettingsService;

        this.globalRules = globalRules;
        this.workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
    }


    @Override public void resourceChanged(IResourceChangeEvent event) {
        final Collection<IProject> newProjects = Lists.newLinkedList();
        final Collection<IProject> openedProjects = Lists.newLinkedList();

        if(event.getType() == IResourceChangeEvent.PRE_CLOSE || event.getType() == IResourceChangeEvent.PRE_DELETE) {
            final IResource resource = event.getResource();
            final FileObject location = resourceService.resolve(resource);
            unloadJob(location).schedule();
        }

        final IResourceDelta delta = event.getDelta();
        if(delta == null) {
            return;
        }

        try {
            delta.accept(new IResourceDeltaVisitor() {
                public boolean visit(IResourceDelta delta) throws CoreException {
                    final IResource resource = delta.getResource();
                    if(resource instanceof IProject) {
                        final IProject project = (IProject) resource;
                        final int kind = delta.getKind();
                        final int flags = delta.getFlags();
                        if(flags == IResourceDelta.OPEN) {
                            openedProjects.add(project);
                        } else if(kind == IResourceDelta.ADDED && project.isAccessible()) {
                            newProjects.add(project);
                        }
                    }

                    // Only continue for the workspace root
                    return resource.getType() == IResource.ROOT;
                }
            });
        } catch(CoreException e) {
            logger.error("Error occurred during project opened notification", e);
        }

        for(IProject project : newProjects) {
            if(!isLanguageProject(project)) {
                return;
            }
            final FileObject location = resourceService.resolve(project);
            loadJob(location, true).schedule();
        }

        for(IProject project : openedProjects) {
            if(!isLanguageProject(project)) {
                return;
            }
            final FileObject location = resourceService.resolve(project);
            loadJob(location, true).schedule();
        }
    }

    /**
     * Loads language components and dialects in given Eclipse project.
     * 
     * @param project
     *            Eclipse project to load from.
     * @param skipUnavailable
     *            If unavailable requests should be skipped.
     */
    public void load(IProject project, boolean skipUnavailable) {
        final FileObject location = resourceService.resolve(project);
        load(location, skipUnavailable);
    }

    /**
     * Loads language components and dialects at given location
     * 
     * @param location
     *            Location to load from.
     * @param skipUnavailable
     *            If unavailable requests should be skipped.
     */
    public void load(FileObject location, boolean skipUnavailable) {
        try {
            final Iterable<INewLanguageDiscoveryRequest> requests = languageDiscoveryService.request(location);
            if(skipUnavailable) {
                for(INewLanguageDiscoveryRequest request : requests) {
                    if(!request.available()) {
                        logger.debug("Skipping loading language component at {}, "
                            + "some resources are unavailable or the configuration is invalid", location);
                        continue;
                    }
                    languageDiscoveryService.discover(request);
                }
            } else {
                languageDiscoveryService.discover(requests);
            }
        } catch(MetaborgException e) {
            logger.error("Could not discover language at location {}", e, location);
        }

        try {
            final Iterable<FileObject> resources = ResourceUtils.find(location);
            final Iterable<ResourceChange> creations = ResourceUtils.toChanges(resources, ResourceChangeKind.Create);
            dialectProcessor.update(location, creations);
        } catch(IOException e) {
            logger.error("Could not discover dialects at location {}", e, location);
        }
    }

    /**
     * Creates a job that loads language components and dialects at given location.
     * 
     * @param location
     *            Location to load from.
     * @param skipUnavailable
     *            If unavailable requests should be skipped.
     * @return Job, must still be scheduled.
     */
    public Job loadJob(FileObject location, boolean skipUnavailable) {
        final LoadLanguageJob job = new LoadLanguageJob(this, location, skipUnavailable);
        job.setRule(new MultiRule(new ISchedulingRule[] { workspaceRoot, globalRules.startupReadLock(),
            globalRules.languageServiceLock() }));
        return job;
    }

    /**
     * Loads all languages and dialects in plugins and in workspace projects.
     */
    public void loadAll() {
        logger.debug("Loading languages from plugins");
        final IExtensionRegistry registry = Platform.getExtensionRegistry();
        final IExtensionPoint point = registry.getExtensionPoint("org.metaborg.spoofax.eclipse.language");
        for(IConfigurationElement config : point.getConfigurationElements()) {
            if(config.getName().equals("language")) {
                final String relativeLocation = config.getAttribute("location");
                final String contributor = config.getDeclaringExtension().getContributor().getName();
                try {
                    final Bundle bundle = Platform.getBundle(contributor);
                    final File bundleLocationFile = FileLocator.getBundleFile(bundle);
                    final FileObject bundleLocation = resourceService.resolve(bundleLocationFile);
                    final FileObject location = bundleLocation.resolveFile(relativeLocation);
                    load(location, false);
                } catch(IOException e) {
                    logger.error("Could not load language from {} in plugin {}", e, relativeLocation, contributor);
                }
            }
        }

        logger.debug("Loading languages and dialects from workspace projects");
        for(final IProject project : workspaceRoot.getProjects()) {
            if(project.isOpen() && isLanguageProject(project)) {
                load(project, true);
            }
        }
    }


    /**
     * Unloads language components at given Eclipse project.
     * 
     * @param project
     *            Eclipse project to unload components at.
     */
    public void unload(IProject project) {
        final FileObject resource = resourceService.resolve(project);
        unload(resource);
    }

    /**
     * Unloads language components at given location.
     * 
     * @param location
     *            Location to unload components at.
     */
    public void unload(FileObject location) {
        final ILanguageComponent component = languageService.getComponent(location.getName());
        if(component != null) {
            languageService.remove(component);
        } else {
            logger.debug("Cannot unload component at location {}, there is no component at that location", location);
        }
    }

    /**
     * Creates a job that unloads language components at given location.
     * 
     * @param location
     *            Location to unload components at.
     * @return Job, must still be scheduled.
     */
    public Job unloadJob(FileObject location) {
        final UnloadLanguageJob job = new UnloadLanguageJob(this, location);
        job.setRule(new MultiRule(new ISchedulingRule[] { workspaceRoot, globalRules.startupReadLock(),
            globalRules.languageServiceLock() }));
        return job;
    }


    /**
     * Checks if given Eclipse project is a Spoofax language project.
     * 
     * @param eclipseProject
     *            Eclipse project to check
     * @return True if project is a Spoofax language project, false if not.
     */
    public boolean isLanguageProject(IProject eclipseProject) {
        final FileObject resource = resourceService.resolve(eclipseProject);
        final org.metaborg.core.project.IProject project = projectService.get(resource);
        ILegacyProjectSettings settings = projectSettingsService.get(project);
        if(settings == null) {
            settings = projectSettingsService.get(resource);
        }
        return settings != null;
    }
}
