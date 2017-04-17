package org.metaborg.spoofax.eclipse.util;

import org.eclipse.core.commands.Command;
import org.eclipse.ui.handlers.RegistryToggleState;

public class CommandStateUtils {
    public static boolean toggleState(Command command) {
        return (boolean) command.getState(RegistryToggleState.STATE_ID).getValue();
    }
}
