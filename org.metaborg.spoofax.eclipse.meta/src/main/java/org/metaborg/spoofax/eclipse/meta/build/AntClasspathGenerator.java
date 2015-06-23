package org.metaborg.spoofax.eclipse.meta.build;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang3.SystemUtils;
import org.eclipse.core.runtime.FileLocator;
import org.metaborg.spoofax.eclipse.meta.SpoofaxMetaPlugin;
import org.metaborg.spoofax.eclipse.util.BundleUtils;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.strategoxt.imp.generator.sdf2imp;

import com.google.common.collect.Lists;

public class AntClasspathGenerator {
    private static final Logger logger = LoggerFactory.getLogger(AntClasspathGenerator.class);

    /**
     * @return List of classpath entries generated from installed Eclipse plugins.
     */
    public static URL[] classpaths() throws MalformedURLException {
        final Collection<URL> classpath = strategoClasspaths();
        final Map<String, Bundle> bundles = BundleUtils.bundlesBySymbolicName(SpoofaxMetaPlugin.context());

        final Bundle antBundle = bundles.get("org.apache.ant");
        if(antBundle == null) {
            logger.error("Could not find Ant bundle 'org.apache.ant', language build will probably fail");
        } else {
            try {
                final File file = FileLocator.getBundleFile(antBundle);
                final String path = file.getAbsolutePath();
                final File lib = Paths.get(path, "lib").toFile();
                final File[] jarFiles = lib.listFiles(new FilenameFilter() {
                    @Override public boolean accept(File dir, String name) {
                        return name.endsWith(".jar");
                    }
                });
                for(File jarFile : jarFiles) {
                    classpath.add(jarFile.toURI().toURL());
                }
            } catch(IOException e) {
                logger.error("Error while adding 'org.apache.ant' to classpath for Ant build, "
                    + "language build will probably fail", e);
            }
        }

        for(final Bundle bundle : bundles.values()) {
            try {
                final File file = FileLocator.getBundleFile(bundle);
                final String path = file.getAbsolutePath();
                if(path.endsWith(".jar")) {
                    // An installed JAR plugin.
                    classpath.add(file.toURI().toURL());
                    continue;
                }

                final File targetClasses = Paths.get(path, "target", "classes").toFile();
                final File bin = Paths.get(path, "bin").toFile();
                if(targetClasses.exists()) {
                    // A plugin under development with all its classes in the target/classes directory.
                    classpath.add(targetClasses.toURI().toURL());
                } else if(bin.exists()) {
                    // A plugin under development with all its classes in the bin directory.
                    classpath.add(bin.toURI().toURL());
                } else {
                    // An installed unpacked plugin. Class files are extracted in this directory.
                    classpath.add(file.toURI().toURL());
                }

                // Also include any nested jar files.
                final File[] jarFiles = file.listFiles(new FilenameFilter() {
                    @Override public boolean accept(File dir, String name) {
                        return name.endsWith(".jar");
                    }
                });
                for(File jarFile : jarFiles) {
                    classpath.add(jarFile.toURI().toURL());
                }
            } catch(IOException e) {
                logger.error("Error while creating classpath for Ant build", e);
            }
        }

        return classpath.toArray(new URL[0]);
    }

    private static Collection<URL> strategoClasspaths() throws MalformedURLException {
        final Collection<URL> classpaths = Lists.newLinkedList();
        classpaths.add(new File(strategoJar()).toURI().toURL());
        classpaths.add(Paths.get(jars(), "make_permissive.jar").toFile().toURI().toURL());

        return classpaths;
    }

    private static String strategoJar() {
        String result = org.strategoxt.lang.Context.class.getProtectionDomain().getCodeSource().getLocation().getFile();
        if(SystemUtils.IS_OS_WINDOWS) {
            // Fix path on Windows.
            result = result.substring(1);
        }
        if(!result.endsWith(".jar")) {
            // Ensure correct JAR file at development time.
            String result2 = result + "/../strategoxt.jar";
            if(new File(result2).exists())
                return result2;
            result2 = result + "/java/strategoxt.jar";
            if(new File(result2).exists())
                return result2;
        }
        return result;
    }

    private static String jars() {
        final String generatorPath = sdf2imp.class.getProtectionDomain().getCodeSource().getLocation().getFile();
        return Paths.get(new File(generatorPath).getAbsolutePath(), "dist").toFile().getAbsolutePath();
    }
}
