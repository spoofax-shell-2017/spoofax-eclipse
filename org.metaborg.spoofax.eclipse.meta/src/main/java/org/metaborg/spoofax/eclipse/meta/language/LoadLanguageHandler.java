package org.metaborg.spoofax.eclipse.meta.language;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.metaborg.spoofax.eclipse.language.EclipseLanguageLoader;
import org.metaborg.spoofax.eclipse.meta.SpoofaxMetaPlugin;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.eclipse.util.AbstractHandlerUtils;

import com.google.inject.Injector;

public class LoadLanguageHandler extends AbstractHandler {
    private final IEclipseResourceService resourceService;
    private final EclipseLanguageLoader loader;


    public LoadLanguageHandler() {
        final Injector injector = SpoofaxMetaPlugin.injector();
        this.resourceService = injector.getInstance(IEclipseResourceService.class);
        this.loader = injector.getInstance(EclipseLanguageLoader.class);
    }


    @Override public Object execute(ExecutionEvent event) throws ExecutionException {
        final IProject project = AbstractHandlerUtils.toProject(event);
        if(project == null) {
            return null;
        }

        final FileObject location = resourceService.resolve(project);
        loader.loadJob(location, false).schedule();

        return null;
    }
}
