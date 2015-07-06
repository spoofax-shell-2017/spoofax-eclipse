package org.metaborg.spoofax.eclipse;

import org.eclipse.ui.IStartup;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.metaborg.core.processing.IProcessorRunner;
import org.metaborg.spoofax.eclipse.editor.IEclipseEditorRegistryInternal;
import org.metaborg.spoofax.eclipse.logging.LoggingConfiguration;
import org.metaborg.spoofax.eclipse.processing.EclipseProcessor;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class SpoofaxPlugin extends AbstractUIPlugin implements IStartup {
    public static final String id = "org.metaborg.spoofax.eclipse";

    private static volatile SpoofaxPlugin plugin;
    private static volatile Logger logger;
    private static volatile Injector injector;


    @Override public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;

        LoggingConfiguration.configure(SpoofaxPlugin.class, "/logback.xml");

        logger = LoggerFactory.getLogger(SpoofaxPlugin.class);
        logger.debug("Starting Spoofax plugin");

        injector = Guice.createInjector(new SpoofaxEclipseModule());
        // Eagerly initialize processor runner so that language changes are processed.
        injector.getInstance(IProcessorRunner.class);
        // Eagerly register editor registry so that editor changes are processsed.
        injector.getInstance(IEclipseEditorRegistryInternal.class).register();
        // Discover languages at startup.
        injector.getInstance(EclipseProcessor.class).discoverLanguages();
    }

    @Override public void stop(BundleContext context) throws Exception {
        logger.debug("Stopping Spoofax plugin");
        logger = null;

        injector = null;
        plugin = null;
        super.stop(context);
    }

    @Override public void earlyStartup() {
        /*
         * Ignore early startup, but this forces this plugin to be started when Eclipse starts. This is required for
         * setting up editor associations for languages in plugins, and languages in the workspace, as soon as possible.
         */
    }


    public static SpoofaxPlugin plugin() {
        return plugin;
    }

    public static Injector injector() {
        return injector;
    }
}
