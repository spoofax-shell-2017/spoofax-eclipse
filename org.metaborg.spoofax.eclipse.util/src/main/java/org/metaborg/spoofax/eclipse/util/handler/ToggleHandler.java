package org.metaborg.spoofax.eclipse.util.handler;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

public class ToggleHandler extends NoneHandler {
    @Override public Object execute(ExecutionEvent event) throws ExecutionException {
        final Command command = event.getCommand();
        HandlerUtil.toggleCommandState(command);
        return null;
    }
}
