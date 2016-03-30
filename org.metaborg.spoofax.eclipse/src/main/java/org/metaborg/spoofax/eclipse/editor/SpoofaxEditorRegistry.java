package org.metaborg.spoofax.eclipse.editor;

import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.spoofax.interpreter.terms.IStrategoTerm;

import com.google.inject.Inject;

/**
 * Typedef class for {@link EditorRegistry} with Spoofax interfaces.
 */
public class SpoofaxEditorRegistry extends EditorRegistry<IStrategoTerm> {
    @Inject public SpoofaxEditorRegistry(IEclipseResourceService resourceService) {
        super(resourceService);
    }
}
