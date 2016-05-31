package org.metaborg.spoofax.eclipse.meta.bootstrap;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.language.ILanguageComponent;
import org.metaborg.core.language.ILanguageDiscoveryRequest;
import org.metaborg.core.language.ILanguageDiscoveryService;
import org.metaborg.core.language.ILanguageService;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.eclipse.util.Nullable;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

public class BootstrapLoadLangChange implements IBootstrapChange {
    private static final ILogger logger = LoggerUtils.logger(BootstrapLoadLangChange.class);

    private final IEclipseResourceService resourceService;
    private final ILanguageService languageService;
    private final ILanguageDiscoveryService languageDiscoveryService;

    private final FileObject binary;

    private @Nullable Iterable<ILanguageComponent> components;


    public BootstrapLoadLangChange(IEclipseResourceService resourceService, ILanguageService languageService,
        ILanguageDiscoveryService languageDiscoveryService, FileObject binary) {
        this.resourceService = resourceService;
        this.languageService = languageService;
        this.languageDiscoveryService = languageDiscoveryService;
        this.binary = binary;
    }


    @Override public void apply() throws MetaborgException {
        final FileObject zipBinary = resourceService.resolve("zip:" + binary.getName().getURI() + "!/");
        logger.info("Reloading language implementation at {}", zipBinary);
        final Iterable<ILanguageDiscoveryRequest> requests = languageDiscoveryService.request(zipBinary);
        components = languageDiscoveryService.discover(requests);
    }

    @Override public void unapply() throws MetaborgException {
        if(components == null) {
            return;
        }

        for(ILanguageComponent component : components) {
            try {
                languageService.remove(component);
                logger.info("Unloaded language component {}", component);
            } catch(IllegalStateException e) {
                // Ignore, language component could have already been unloaded.
            }
        }

        components = null;
    }
}
