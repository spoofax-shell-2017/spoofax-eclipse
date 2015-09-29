package org.metaborg.spoofax.eclipse.transform;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.jface.action.IContributionItem;
import org.metaborg.core.language.ILanguageIdentifierService;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.spoofax.eclipse.SpoofaxPlugin;
import org.metaborg.spoofax.eclipse.editor.IEclipseEditor;
import org.metaborg.spoofax.eclipse.editor.IEclipseEditorRegistry;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;

public class TransformMenuContribution extends MenuContribution {
    private static final Logger logger = LoggerFactory.getLogger(TransformMenuContribution.class);

    private final IEclipseResourceService resourceService;
    private final ILanguageIdentifierService languageIdentifier;
    private final IEclipseEditorRegistry latestEditorListener;


    public TransformMenuContribution() {
        final Injector injector = SpoofaxPlugin.injector();
        this.resourceService = injector.getInstance(IEclipseResourceService.class);
        this.languageIdentifier = injector.getInstance(ILanguageIdentifierService.class);
        this.latestEditorListener = injector.getInstance(IEclipseEditorRegistry.class);
    }


    @Override protected IContributionItem[] getContributionItems() {
        final IEclipseEditor editor = latestEditorListener.previousEditor();
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
