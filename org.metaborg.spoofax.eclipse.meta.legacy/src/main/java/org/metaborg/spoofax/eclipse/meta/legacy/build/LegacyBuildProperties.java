package org.metaborg.spoofax.eclipse.meta.legacy.build;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collection;

import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.strategoxt.imp.generator.sdf2imp;

import com.google.common.collect.Lists;

public class LegacyBuildProperties {
    private static final Logger logger = LoggerFactory.getLogger(LegacyBuildProperties.class);

    
    public static Collection<URL> classpaths() throws MalformedURLException {
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
