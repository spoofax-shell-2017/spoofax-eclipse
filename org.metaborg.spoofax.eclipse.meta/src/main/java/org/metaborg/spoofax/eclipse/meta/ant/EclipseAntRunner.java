package org.metaborg.spoofax.eclipse.meta.ant;

import java.io.File;
import java.net.URL;
import java.util.Map;

import org.apache.commons.vfs2.FileObject;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.BuildLogger;
import org.eclipse.ant.core.AntRunner;
import org.eclipse.core.runtime.CoreException;
import org.metaborg.core.processing.ICancellationToken;
import org.metaborg.core.resource.IResourceService;
import org.metaborg.spoofax.eclipse.processing.EclipseCancellationToken;
import org.metaborg.spoofax.eclipse.util.Nullable;
import org.metaborg.spoofax.meta.core.ant.IAntRunner;


public class EclipseAntRunner implements IAntRunner {
    private final AntRunner runner;


    public EclipseAntRunner(IResourceService resourceService, FileObject antFile, FileObject baseDir,
        Map<String, String> properties, @Nullable URL[] classpaths, @Nullable BuildListener listener) {
        final File localAntFile = resourceService.localFile(antFile);
        final File localBaseDir = resourceService.localPath(baseDir);

        runner = new AntRunner();
        runner.setBuildFileLocation(localAntFile.getPath());
        properties.put("basedir", localBaseDir.getPath());
        runner.addUserProperties(properties);
        runner.setCustomClasspath(classpaths);
        if(listener != null) {
            final String name = listener.getClass().getName();
            if(listener instanceof BuildLogger) {
                runner.addBuildLogger(name);
            } else {
                runner.addBuildListener(name);
            }
        }
    }


    @Override public void execute(String target, @Nullable ICancellationToken cancellationToken) throws CoreException {
        runner.setExecutionTargets(new String[] { target });

        if(cancellationToken instanceof EclipseCancellationToken) {
            final EclipseCancellationToken eclipseCancellationToken = (EclipseCancellationToken) cancellationToken;
            runner.run(eclipseCancellationToken.monitor);
        } else {
            runner.run();
        }
    }
}
