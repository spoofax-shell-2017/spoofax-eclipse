package org.metaborg.spoofax.eclipse.transform;

import java.util.Collection;
import java.util.Set;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.metaborg.core.language.ILanguageIdentifierService;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.spoofax.eclipse.SpoofaxPlugin;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.eclipse.util.SelectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Injector;

public class ContextMenuContribution extends MenuContribution {
    private static final Logger logger = LoggerFactory.getLogger(ContextMenuContribution.class);

    private final IEclipseResourceService resourceService;
    private final ILanguageIdentifierService languageIdentifier;


    public ContextMenuContribution() {
        final Injector injector = SpoofaxPlugin.injector();
        this.resourceService = injector.getInstance(IEclipseResourceService.class);
        this.languageIdentifier = injector.getInstance(ILanguageIdentifierService.class);
    }


    @Override protected IContributionItem[] getContributionItems() {
        final IWorkbench workbench = PlatformUI.getWorkbench();
        final IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
        if(window == null) {
            logger.debug("Cannot create menu items; there is no active workbench window");
            return new IContributionItem[0];
        }
        final ISelectionService selectionService = window.getSelectionService();

        final ISelection selection = selectionService.getSelection();
        if(selection == null) {
            logger.debug("Cannot create menu items; there is no selection");
            return new IContributionItem[0];
        }

        final IStructuredSelection structuredSelection = SelectionUtils.toStructured(selection);
        if(structuredSelection == null) {
            logger.debug("Cannot create menu items; selection is not a structed selection");
            return new IContributionItem[0];
        }

        final Iterable<IResource> resources = SelectionUtils.toResources(structuredSelection);
        final Set<ILanguageImpl> languages = Sets.newHashSet();
        for(IResource eclipseResource : resources) {
            final FileObject resource = resourceService.resolve(eclipseResource);
            final ILanguageImpl language = languageIdentifier.identify(resource);
            if(language != null) {
                languages.add(language);
            }
        }

        final Collection<IContributionItem> items = Lists.newLinkedList();
        for(ILanguageImpl language : languages) {
            final MenuManager menuManager = new MenuManager(language.belongsTo().name());
            final IContributionItem[] nestedItems = getContributionItems(language, false);
            for(IContributionItem item : nestedItems) {
                menuManager.add(item);
            }
            items.add(menuManager);
        }
        return items.toArray(new IContributionItem[0]);
    }
}
