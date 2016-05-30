package org.metaborg.spoofax.eclipse.meta.bootstrap;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.language.ILanguageDiscoveryRequest;
import org.metaborg.core.language.ILanguageDiscoveryService;
import org.metaborg.spoofax.eclipse.meta.SpoofaxMetaPlugin;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.eclipse.util.StatusUtils;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.resource.FileSelectorUtils;

/**
 * Job for discovering languages and dialects from plugins.
 */
public class DiscoverLanguagesFromBootstrappingJob extends Job {
    private static final ILogger logger = LoggerUtils.logger(DiscoverLanguagesFromBootstrappingJob.class);

    private final IEclipseResourceService resourceService;
    private final ILanguageDiscoveryService languageDiscoveryService;


    public DiscoverLanguagesFromBootstrappingJob(IEclipseResourceService resourceService,
        ILanguageDiscoveryService languageDiscoveryService) {
        super("Loading all Spoofax languages from bootstrapped binaries");
        setPriority(Job.LONG);

        this.resourceService = resourceService;
        this.languageDiscoveryService = languageDiscoveryService;
    }


    @Override protected IStatus run(IProgressMonitor monitor) {
        logger.debug("Loading all Spoofax languages from bootstrapped binaries");


        final FileObject[] binaries;
        try {
            final IPath statePath = SpoofaxMetaPlugin.plugin().getStateLocation();
            final FileObject stateDir = resourceService.resolve(statePath.toString());
            final FileObject storeDir = stateDir.resolveFile("bootstrap");
            binaries = storeDir.findFiles(FileSelectorUtils.extension("spoofax-language"));
        } catch(IllegalStateException | FileSystemException e) {
            final String message = "Getting bootstrapped language binaries failed unexpectedly";
            logger.error(message, e);
            return StatusUtils.silentError(message, e);
        }

        for(FileObject binary : binaries) {
            try {
                final FileObject zipBinary = resourceService.resolve("zip:" + binary.getName().getURI() + "!/");
                final Iterable<ILanguageDiscoveryRequest> requests = languageDiscoveryService.request(zipBinary);
                languageDiscoveryService.discover(requests);
            } catch(MetaborgException e) {
                final String message =
                    logger.format("Loading bootstrapped language binary {} failed unexpectedly", e, binary);
                logger.error(message, e);
                return StatusUtils.silentError(message, e);
            }
        }

        return StatusUtils.success();
    }
}
