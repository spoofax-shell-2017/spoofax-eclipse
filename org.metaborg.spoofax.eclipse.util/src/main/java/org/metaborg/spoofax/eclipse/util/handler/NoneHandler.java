package org.metaborg.spoofax.eclipse.util.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

public class NoneHandler extends AbstractHandler {
    @Override public boolean isEnabled() {
        return true;
    }

    @Override public Object execute(ExecutionEvent event) throws ExecutionException {
        return null;
    }
}
