package org.metaborg.spoofax.eclipse.language;

import java.io.File;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.Job;
import org.metaborg.core.language.ILanguageDiscoveryService;
import org.metaborg.core.language.dialect.IDialectProcessor;
import org.metaborg.core.project.IProjectService;
import org.metaborg.core.resource.ResourceChange;
import org.metaborg.core.resource.ResourceChangeKind;
import org.metaborg.core.resource.ResourceUtils;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.eclipse.util.StatusUtils;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Job for discovering languages at startup.
 */
public class DiscoverLanguagesJob extends Job {
    private static final Logger logger = LoggerFactory.getLogger(DiscoverLanguagesJob.class);

    private final IEclipseResourceService resourceService;
    private final ILanguageDiscoveryService languageDiscoveryService;
    private final IProjectService projectService;
    private final IDialectProcessor dialectProcessor;


    public DiscoverLanguagesJob(IEclipseResourceService resourceService,
        ILanguageDiscoveryService languageDiscoveryService, IProjectService projectService,
        IDialectProcessor dialectProcessor) {
        super("Loading all Spoofax languages in workspace");
        this.projectService = projectService;
        setPriority(Job.LONG);

        this.resourceService = resourceService;
        this.languageDiscoveryService = languageDiscoveryService;
        this.dialectProcessor = dialectProcessor;
    }


    @Override protected IStatus run(IProgressMonitor monitor) {
        logger.debug("Running discover languages job");
        logger.debug("Loading static languages");
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
                    languageDiscoveryService.discover(location);
                } catch(Exception e) {
                    final String message =
                        String.format("Could not load language from %s in plugin %s", relativeLocation, contributor);
                    logger.error(message, e);
                }
            }
        }

        logger.debug("Loading dynamic languages and dialects");
        for(final IProject eclipseProject : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
            if(eclipseProject.isOpen()) {
                final FileObject location = resourceService.resolve(eclipseProject);
                try {
                    languageDiscoveryService.discover(location);
                } catch(Exception e) {
                    final String message = String.format("Could not load language at location %s", location);
                    logger.error(message, e);
                }

                try {
                    final org.metaborg.core.project.IProject project = projectService.get(location);
                    final Iterable<FileObject> resources = ResourceUtils.find(location);
                    final Iterable<ResourceChange> creations =
                        ResourceUtils.toChanges(resources, ResourceChangeKind.Create);
                    dialectProcessor.update(project, creations);
                } catch(Exception e) {
                    final String message = String.format("Could not load dialects at location %s", location);
                    logger.error(message, e);
                }
            }
        }

        return StatusUtils.success();
    }
}
