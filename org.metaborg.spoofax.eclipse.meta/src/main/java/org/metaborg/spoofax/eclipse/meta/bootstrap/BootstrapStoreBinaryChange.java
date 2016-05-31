package org.metaborg.spoofax.eclipse.meta.bootstrap;

import org.apache.commons.vfs2.AllFileSelector;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.eclipse.core.runtime.IPath;
import org.metaborg.core.language.LanguageIdentifier;
import org.metaborg.spoofax.eclipse.meta.SpoofaxMetaPlugin;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.eclipse.util.Nullable;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

public class BootstrapStoreBinaryChange implements IBootstrapChange {
    private static final ILogger logger = LoggerUtils.logger(BootstrapStoreBinaryChange.class);

    private final IEclipseResourceService resourceService;

    private final BootstrapProject project;
    private final FileObject binary;

    private @Nullable FileObject storeFile;


    public BootstrapStoreBinaryChange(IEclipseResourceService resourceService, BootstrapProject project,
        FileObject binary) {
        this.resourceService = resourceService;
        this.project = project;
        this.binary = binary;
    }


    @Override public void apply() throws FileSystemException {
        final IPath statePath = SpoofaxMetaPlugin.plugin().getStateLocation();
        final FileObject stateDir = resourceService.resolve(statePath.toString());
        final LanguageIdentifier id = project.identifier();
        // @formatter:off
        storeFile = stateDir
            .resolveFile("bootstrap")
            .resolveFile(id.groupId)
            .resolveFile(id.id)
            .resolveFile(id.version.toString())
            .resolveFile(binary.getName().getBaseName())
            ;
        // @formatter:on
        logger.info("Storing binary for {} at {}", project, storeFile);
        storeFile.createFile();
        storeFile.copyFrom(binary, new AllFileSelector());
    }

    @Override public void unapply() throws FileSystemException {
        if(storeFile == null) {
            return;
        }

        logger.info("Deleting binary for {} at {}", project, storeFile);
        storeFile.delete();

        storeFile = null;
    }

    public @Nullable FileObject storeFile() {
        return storeFile;
    }
}
