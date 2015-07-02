package org.metaborg.spoofax.eclipse.transform;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.metaborg.core.language.ILanguage;
import org.metaborg.core.transform.ITransformerGoal;
import org.metaborg.core.transform.NamedGoal;
import org.metaborg.core.transform.TransformResult;
import org.metaborg.spoofax.core.transform.stratego.IStrategoTransformerResultHandler;
import org.metaborg.spoofax.core.transform.stratego.StrategoTransformerCommon;
import org.metaborg.spoofax.core.transform.stratego.menu.Action;
import org.metaborg.spoofax.core.transform.stratego.menu.MenusFacet;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.eclipse.util.EditorUtils;
import org.spoofax.interpreter.terms.IStrategoTerm;

import com.google.inject.Inject;

public class OpenEditorResultHandler implements IStrategoTransformerResultHandler {
    private final IEclipseResourceService resourceService;

    private final StrategoTransformerCommon transformer;


    @Inject public OpenEditorResultHandler(IEclipseResourceService resourceService,
        StrategoTransformerCommon transformer) {
        this.resourceService = resourceService;
        this.transformer = transformer;
    }


    @Override public void handle(TransformResult<?, IStrategoTerm> result, ITransformerGoal goal) {
        final FileObject resource = transformer.builderWriteResult(result.result, result.context.location());
        if(openEditor(resource, result.context.language(), goal)) {
            final IResource eclipseResource = resourceService.unresolve(resource);
            if(eclipseResource instanceof IFile) {
                final IFile file = (IFile) eclipseResource;
                EditorUtils.openEditor(file);
            }
        }
    }

    private boolean openEditor(FileObject resource, ILanguage language, ITransformerGoal goal) {
        if(resource == null) {
            return false;
        }

        if(goal instanceof NamedGoal) {
            final MenusFacet facet = language.facet(MenusFacet.class);
            if(facet == null) {
                return false;
            }
            final NamedGoal namedGoal = (NamedGoal) goal;
            final Action action = facet.action(namedGoal.name);
            if(action == null) {
                return false;
            }
            return action.flags.openEditor;
        }
        return false;
    }
}
