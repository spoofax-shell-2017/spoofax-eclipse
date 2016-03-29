package org.metaborg.spoofax.eclipse.logging;

import java.io.InputStream;

import org.slf4j.ILoggerFactory;
import org.slf4j.impl.StaticLoggerBinder;

import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

public class LoggingConfiguration {
    public static void configure(Class<?> clazz, String location) {
        final ILoggerFactory loggerFactory = StaticLoggerBinder.getSingleton().getLoggerFactory();
        final ContextBase context = (ContextBase) loggerFactory;
        try {
            final JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            context.reset();
            final InputStream configStream = clazz.getResourceAsStream(location);
            configurator.doConfigure(configStream);
        } catch(JoranException je) {
            // Ignore exception, StatusPrinter will handle it.
        }
        StatusPrinter.printInCaseOfErrorsOrWarnings(context);
    }
}
