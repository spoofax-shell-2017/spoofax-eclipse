package org.metaborg.spoofax.eclipse.nature;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.metaborg.spoofax.eclipse.util.NatureUtils;
import org.metaborg.spoofax.eclipse.util.handler.AbstractHandlerUtils;

public class RemoveNatureHandler extends AbstractHandler {
    @Override public Object execute(ExecutionEvent event) throws ExecutionException {
        final IProject project = AbstractHandlerUtils.toProject(event);
        if(project == null)
            return null;

        try {
            NatureUtils.removeFrom(SpoofaxNature.id, project, null);
        } catch(CoreException e) {
            throw new ExecutionException("Cannot add Spoofax nature", e);
        }

        return null;
    }
}
