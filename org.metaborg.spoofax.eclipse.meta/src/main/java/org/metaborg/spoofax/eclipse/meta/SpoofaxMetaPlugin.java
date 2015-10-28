package org.metaborg.spoofax.eclipse.meta;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.metaborg.spoofax.eclipse.SpoofaxPlugin;
import org.metaborg.spoofax.eclipse.meta.language.MetaProjectListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;

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

        ResourcesPlugin.getWorkspace().addResourceChangeListener(injector.getInstance(MetaProjectListener.class));
    }

    @Override public void stop(BundleContext context) throws Exception {
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(injector.getInstance(MetaProjectListener.class));

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

    public static ClassLoader classLoader() {
        return bundleContext.getBundle().adapt(BundleWiring.class).getClassLoader();
    }
}
