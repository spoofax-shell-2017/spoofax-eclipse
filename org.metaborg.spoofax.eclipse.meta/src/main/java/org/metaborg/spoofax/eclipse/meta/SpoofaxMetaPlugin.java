package org.metaborg.spoofax.eclipse.meta;

import org.eclipse.ui.IStartup;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.metaborg.spoofax.eclipse.EclipseModulePluginLoader;
import org.metaborg.spoofax.eclipse.SpoofaxPlugin;
import org.metaborg.spoofax.eclipse.meta.language.MetaLanguageLoader;
import org.metaborg.spoofax.meta.core.SpoofaxMeta;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;

public class SpoofaxMetaPlugin extends AbstractUIPlugin implements IStartup {
    public static final String id = "org.metaborg.spoofax.eclipse.meta";

    private static volatile SpoofaxMetaPlugin plugin;
    private static volatile BundleContext bundleContext;
    private static volatile Logger logger;
    private static volatile SpoofaxMeta spoofaxMeta;
    private static volatile Injector injector;


    @Override public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        bundleContext = context;

        logger = LoggerFactory.getLogger(SpoofaxMetaPlugin.class);
        logger.debug("Starting Spoofax meta plugin");

        spoofaxMeta = new SpoofaxMeta(SpoofaxPlugin.spoofax(), new EclipseModulePluginLoader(id + ".module"),
            new SpoofaxEclipseMetaModule());
        injector = spoofaxMeta.injector;

        // Discover language components and dialects from language specifications of workspace projects at startup.
        injector.getInstance(MetaLanguageLoader.class).loadFromProjectsJob().schedule();
    }

    @Override public void stop(BundleContext context) throws Exception {
        logger.debug("Stopping Spoofax meta plugin");
        injector = null;
        spoofaxMeta.close();
        spoofaxMeta = null;
        logger = null;
        bundleContext = null;
        plugin = null;
        super.stop(context);
    }

    @Override public void earlyStartup() {
        /*
         * Ignore early startup, but this forces this plugin to be started when Eclipse starts. This is required for
         * setting up editor associations for language components and dialects of language specifications in open
         * projects as soon as possible.
         */
    }


    public static SpoofaxMetaPlugin plugin() {
        return plugin;
    }

    public static BundleContext context() {
        return bundleContext;
    }

    public static SpoofaxMeta spoofaxMeta() {
        return spoofaxMeta;
    }

    public static Injector injector() {
        return injector;
    }

    public static ClassLoader classLoader() {
        return bundleContext.getBundle().adapt(BundleWiring.class).getClassLoader();
    }
}
