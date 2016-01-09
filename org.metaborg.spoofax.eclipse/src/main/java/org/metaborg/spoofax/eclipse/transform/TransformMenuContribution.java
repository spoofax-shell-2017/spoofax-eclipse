package org.metaborg.spoofax.eclipse.transform;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.jface.action.IContributionItem;
import org.metaborg.core.language.ILanguageIdentifierService;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.spoofax.eclipse.SpoofaxPlugin;
import org.metaborg.spoofax.eclipse.editor.IEclipseEditor;
import org.metaborg.spoofax.eclipse.editor.IEclipseEditorRegistry;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.terms.IStrategoTerm;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

public class TransformMenuContribution extends MenuContribution {
    private static final ILogger logger = LoggerUtils.logger(TransformMenuContribution.class);

    private final IEclipseResourceService resourceService;
    private final ILanguageIdentifierService languageIdentifier;
    private final IEclipseEditorRegistry<?> editorRegistry;


    public TransformMenuContribution() {
        final Injector injector = SpoofaxPlugin.injector();
        this.resourceService = injector.getInstance(IEclipseResourceService.class);
        this.languageIdentifier = injector.getInstance(ILanguageIdentifierService.class);
        this.editorRegistry =
            injector.getInstance(Key.get(new TypeLiteral<IEclipseEditorRegistry<IStrategoTerm>>() {}));
    }


    @Override protected IContributionItem[] getContributionItems() {
        final IEclipseEditor<?> editor = editorRegistry.previousEditor();
        if(editor == null) {
            logger.debug("Cannot create menu items; there is no latest active editor");
            return new IContributionItem[0];
        }

        final FileObject resource = resourceService.resolve(editor.input());
        if(resource == null) {
            logger.error("Cannot create menu items; cannot resolve input resource for {}", editor);
            return new IContributionItem[0];
        }

        final ILanguageImpl language = languageIdentifier.identify(resource);
        if(language == null) {
            logger.error("Cannot create menu items; cannot identify language for {}", resource);
            return new IContributionItem[0];
        }

        return getContributionItems(language, true);
    }
}
