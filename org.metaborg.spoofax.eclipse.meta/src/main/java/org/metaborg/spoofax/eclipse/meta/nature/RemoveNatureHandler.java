package org.metaborg.spoofax.eclipse.meta.nature;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.metaborg.spoofax.eclipse.util.AbstractHandlerUtils;

public class RemoveNatureHandler extends AbstractHandler {
    @Override public Object execute(ExecutionEvent event) throws ExecutionException {
        final IProject project = AbstractHandlerUtils.toProject(event);
        if(project == null) {
            return null;
        }

        final RemoveNatureJob job = new RemoveNatureJob(project);
        job.schedule();

        return null;
    }
}
