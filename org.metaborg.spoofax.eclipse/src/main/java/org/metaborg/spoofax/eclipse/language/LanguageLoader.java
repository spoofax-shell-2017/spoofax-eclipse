package org.metaborg.spoofax.eclipse.language;

import java.io.File;
import java.io.IOException;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.language.ILanguageComponent;
import org.metaborg.core.language.ILanguageDiscoveryRequest;
import org.metaborg.core.language.ILanguageDiscoveryService;
import org.metaborg.core.language.ILanguageService;
import org.metaborg.core.language.dialect.IDialectProcessor;
import org.metaborg.core.resource.ResourceChange;
import org.metaborg.core.resource.ResourceChangeKind;
import org.metaborg.core.resource.ResourceUtils;
import org.metaborg.spoofax.eclipse.job.GlobalSchedulingRules;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.osgi.framework.Bundle;

import com.google.inject.Inject;

/**
 * Discovers all languages in plugins and workspace projects, and discovers languages when a project is opened.
 */
public class LanguageLoader {
    private static final ILogger logger = LoggerUtils.logger(LanguageLoader.class);

    private final IEclipseResourceService resourceService;
    private final ILanguageService languageService;
    private final ILanguageDiscoveryService languageDiscoveryService;
    private final IDialectProcessor dialectProcessor;

    private final GlobalSchedulingRules globalRules;
    private final IWorkspaceRoot workspaceRoot;


    @Inject public LanguageLoader(IEclipseResourceService resourceService, ILanguageService languageService,
        ILanguageDiscoveryService languageDiscoveryService, IDialectProcessor dialectProcessor,
        GlobalSchedulingRules globalRules) {
        this.resourceService = resourceService;
        this.languageService = languageService;
        this.languageDiscoveryService = languageDiscoveryService;
        this.dialectProcessor = dialectProcessor;
        this.globalRules = globalRules;
        this.workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
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
     * Loads language components and dialects at given location.
     * 
     * @param location
     *            Location to load from.
     * @param skipUnavailable
     *            If unavailable requests should be skipped.
     */
    public void load(FileObject location, boolean skipUnavailable) {
        try {
            final Iterable<ILanguageDiscoveryRequest> requests = languageDiscoveryService.request(location);
            if(skipUnavailable) {
                for(ILanguageDiscoveryRequest request : requests) {
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
        job.setRule(new MultiRule(
            new ISchedulingRule[] { workspaceRoot, globalRules.startupReadLock(), globalRules.languageServiceLock() }));
        return job;
    }

    /**
     * Loads all languages and dialects from plugins.
     */
    public void loadFromPlugins() {
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
    }

    /**
     * Creates a job that loads all languages and dialects from plugins.
     */
    public Job loadFromPluginsJob() {
        final Job job = new DiscoverLanguagesFromPluginsJob(this);
        job.setRule(new MultiRule(new ISchedulingRule[] { workspaceRoot, globalRules.startupWriteLock(),
            globalRules.languageServiceLock() }));
        job.schedule();
        return job;
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
        job.setRule(new MultiRule(
            new ISchedulingRule[] { workspaceRoot, globalRules.startupReadLock(), globalRules.languageServiceLock() }));
        return job;
    }
}
