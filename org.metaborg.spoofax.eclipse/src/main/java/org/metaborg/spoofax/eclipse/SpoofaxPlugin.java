package org.metaborg.spoofax.eclipse;

import org.eclipse.ui.IStartup;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.metaborg.core.processing.IProcessorRunner;
import org.metaborg.spoofax.core.Spoofax;
import org.metaborg.spoofax.eclipse.editor.IEclipseEditorRegistryInternal;
import org.metaborg.spoofax.eclipse.logging.LoggingConfiguration;
import org.metaborg.spoofax.eclipse.processing.EclipseProcessor;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;

public class SpoofaxPlugin extends AbstractUIPlugin implements IStartup {
    public static final String id = "org.metaborg.spoofax.eclipse";

    private static volatile SpoofaxPlugin plugin;
    private static volatile Logger logger;
    private static volatile Spoofax spoofax;
    private static volatile Injector injector;
    private static volatile boolean doneLoading;


    @Override public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;

        LoggingConfiguration.configure(SpoofaxPlugin.class, "/logback.xml");

        logger = LoggerFactory.getLogger(SpoofaxPlugin.class);
        logger.debug("Starting Spoofax plugin");

        spoofax = new Spoofax(new SpoofaxEclipseModule(), new EclipseModulePluginLoader(id + ".module"));
        injector = spoofax.injector;

        // Eagerly initialize processor runner so that language changes are processed.
        injector.getInstance(IProcessorRunner.class);
        // Eagerly register editor registry so that editor changes are processed.
        injector.getInstance(IEclipseEditorRegistryInternal.class).register();
        // Discover language components and dialects from plugins at startup.
        injector.getInstance(EclipseProcessor.class).discoverLanguages();

        doneLoading = true;
    }

    @Override public void stop(BundleContext context) throws Exception {
        logger.debug("Stopping Spoofax plugin");
        doneLoading = false;
        injector = null;
        spoofax = null;
        logger = null;
        plugin = null;
        super.stop(context);
    }

    @Override public void earlyStartup() {
        /*
         * Ignore early startup, but this forces this plugin to be started when Eclipse starts. This is required for
         * setting up editor associations for language components and dialects in plugins as soon as possible.
         */
    }


    public static SpoofaxPlugin plugin() {
        return plugin;
    }

    public static Spoofax spoofax() {
        return spoofax;
    }

    public static Injector injector() {
        return injector;
    }

    public static boolean doneLoading() {
        return doneLoading;
    }
}
