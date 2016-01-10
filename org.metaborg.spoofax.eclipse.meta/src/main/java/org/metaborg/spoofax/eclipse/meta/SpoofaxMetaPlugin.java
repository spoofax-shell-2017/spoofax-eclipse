package org.metaborg.spoofax.eclipse.meta;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.metaborg.spoofax.eclipse.EclipseModulePluginLoader;
import org.metaborg.spoofax.eclipse.SpoofaxPlugin;
import org.metaborg.spoofax.meta.core.SpoofaxMeta;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;

import com.google.inject.Injector;

public class SpoofaxMetaPlugin extends AbstractUIPlugin {
    public static final String id = "org.metaborg.spoofax.eclipse.meta";

    private static SpoofaxMetaPlugin plugin;
    private static BundleContext bundleContext;
    private static SpoofaxMeta spoofaxMeta;
    private static Injector injector;


    @Override public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        bundleContext = context;

        spoofaxMeta = new SpoofaxMeta(SpoofaxPlugin.spoofax(), new SpoofaxEclipseMetaModule(),
            new EclipseModulePluginLoader(id + ".module"));
        injector = spoofaxMeta.injector();
    }

    @Override public void stop(BundleContext context) throws Exception {
        injector = null;
        bundleContext = null;
        plugin = null;
        super.stop(context);
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
