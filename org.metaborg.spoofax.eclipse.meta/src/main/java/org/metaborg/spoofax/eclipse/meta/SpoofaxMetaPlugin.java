package org.metaborg.spoofax.eclipse.meta;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.metaborg.spoofax.eclipse.SpoofaxPlugin;
import org.osgi.framework.BundleContext;

import com.google.inject.Injector;

public class SpoofaxMetaPlugin extends AbstractUIPlugin {
    public static final String id = "org.metaborg.spoofax.eclipse.meta";

    private static SpoofaxMetaPlugin plugin;
    private static BundleContext bundleContext;
    private static Injector injector;


    @Override public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        bundleContext = context;
        injector = SpoofaxPlugin.injector().createChildInjector(new SpoofaxEclipseMetaModule());
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

    public static Injector injector() {
        return injector;
    }
}
