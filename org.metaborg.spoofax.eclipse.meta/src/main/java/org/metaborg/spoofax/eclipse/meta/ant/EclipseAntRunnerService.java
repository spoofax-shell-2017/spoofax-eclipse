package org.metaborg.spoofax.eclipse.meta.ant;

import java.net.URL;
import java.util.Map;

import org.apache.commons.vfs2.FileObject;
import org.apache.tools.ant.BuildListener;
import org.metaborg.core.resource.IResourceService;
import org.metaborg.spoofax.eclipse.util.Nullable;
import org.metaborg.spoofax.meta.core.ant.IAntRunner;
import org.metaborg.spoofax.meta.core.ant.IAntRunnerService;

import com.google.inject.Inject;

public class EclipseAntRunnerService implements IAntRunnerService {
    private final IResourceService resourceService;


    @Inject public EclipseAntRunnerService(IResourceService resourceService) {
        this.resourceService = resourceService;
    }


    @Override public IAntRunner get(FileObject antFile, FileObject baseDir, Map<String, String> properties,
        @Nullable URL[] classpaths, @Nullable BuildListener listener) {
        return new EclipseAntRunner(resourceService, antFile, baseDir, properties, classpaths, listener);
    }
}
