package org.metaborg.spoofax.eclipse.meta.bootstrap;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.metaborg.spoofax.eclipse.meta.SpoofaxMetaPlugin;
import org.metaborg.spoofax.eclipse.util.AbstractHandlerUtils;

import com.google.common.collect.Iterables;
import com.google.inject.Injector;

public class BootstrapSingleHandler extends AbstractHandler {
    private final BootstrapJobFactory bootstrapJobFactory;

    private final IWorkspaceRoot workspaceRoot;


    public BootstrapSingleHandler() {
        final Injector injector = SpoofaxMetaPlugin.injector();
        this.bootstrapJobFactory = injector.getInstance(BootstrapJobFactory.class);

        this.workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
    }


    @Override public Object execute(ExecutionEvent event) throws ExecutionException {
        final Iterable<org.eclipse.core.resources.IProject> eclipseProjects = AbstractHandlerUtils.toProjects(event);
        if(Iterables.isEmpty(eclipseProjects)) {
            return null;
        }

        final BootstrapJob job = bootstrapJobFactory.create(workspaceRoot, eclipseProjects, false);
        job.setRule(workspaceRoot);
        job.schedule();

        return null;
    }
}
