package org.metaborg.spoofax.eclipse.transform;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.menu.IAction;
import org.metaborg.core.menu.IMenuService;
import org.metaborg.core.transform.ITransformerGoal;
import org.metaborg.core.transform.NamedGoal;
import org.metaborg.core.transform.NestedNamedGoal;
import org.metaborg.core.transform.TransformResult;
import org.metaborg.spoofax.core.menu.StrategoTransformAction;
import org.metaborg.spoofax.core.stratego.IStrategoCommon;
import org.metaborg.spoofax.core.transform.IStrategoTransformerResultHandler;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.eclipse.util.EditorUtils;
import org.spoofax.interpreter.terms.IStrategoTerm;

import com.google.inject.Inject;

public class OpenEditorResultHandler implements IStrategoTransformerResultHandler {
    private final IEclipseResourceService resourceService;
    private final IMenuService menuService;

    private final IStrategoCommon transformer;


    @Inject public OpenEditorResultHandler(IEclipseResourceService resourceService, IMenuService menuService,
        IStrategoCommon transformer) {
        this.resourceService = resourceService;
        this.menuService = menuService;
        this.transformer = transformer;
    }


    @Override public void handle(TransformResult<?, IStrategoTerm> result, ITransformerGoal goal) {
        final FileObject resource = transformer.builderWriteResult(result.result, result.context.location());
        if(openEditor(resource, result.context.language(), goal)) {
            final IResource eclipseResource = resourceService.unresolve(resource);
            if(eclipseResource instanceof IFile) {
                final IFile file = (IFile) eclipseResource;
                EditorUtils.open(file);
            }
        }
    }

    private boolean openEditor(FileObject resource, ILanguageImpl language, ITransformerGoal goal) {
        if(resource == null) {
            return false;
        }

        final IAction action;
        if(goal instanceof NamedGoal) {
            final NamedGoal namedGoal = (NamedGoal) goal;
            try {
                action = menuService.action(language, namedGoal.name);
            } catch(MetaborgException e) {
                return false;
            }
        } else if(goal instanceof NestedNamedGoal) {
            final NestedNamedGoal namedGoal = (NestedNamedGoal) goal;
            try {
                action = menuService.nestedAction(language, namedGoal.names);
            } catch(MetaborgException e) {
                return false;
            }
        } else {
            return false;
        }
        
        if(action == null) {
            return false;
        }
        if(action instanceof StrategoTransformAction) {
            final StrategoTransformAction transformAction = (StrategoTransformAction) action;
            return transformAction.flags.openEditor;
        }
        
        return false;
    }
}
