package org.metaborg.spoofax.eclipse.meta.ant;

import java.io.PrintStream;

import org.apache.tools.ant.DefaultLogger;
import org.eclipse.ui.console.MessageConsole;
import org.metaborg.spoofax.eclipse.util.ConsoleUtils;

public class EclipseAntLogger extends DefaultLogger {
    private final MessageConsole console;
    private final PrintStream stream;


    public EclipseAntLogger() {
        super();
        console = ConsoleUtils.get("Spoofax language build");
        stream = new PrintStream(console.newMessageStream());
        console.activate();
    }


    @Override protected void printMessage(String message, PrintStream unusedStream, int priority) {
        /*
         * HACK: instead of setting the output and error streams, just pass our own stream to printMessage. The reason
         * being that Ant likes to change the stream to stdout and stderr after class instantiation, overriding our
         * streams.
         */
        super.printMessage(message, stream, priority);
    }
}