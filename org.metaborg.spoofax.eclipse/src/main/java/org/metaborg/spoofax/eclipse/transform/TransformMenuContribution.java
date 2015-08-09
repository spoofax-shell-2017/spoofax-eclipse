package org.metaborg.spoofax.eclipse.transform;

import java.util.Collection;
import java.util.Map;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.services.IServiceLocator;
import org.metaborg.core.language.ILanguageIdentifierService;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.menu.IAction;
import org.metaborg.core.menu.IMenu;
import org.metaborg.core.menu.IMenuItem;
import org.metaborg.core.menu.IMenuService;
import org.metaborg.core.menu.Separator;
import org.metaborg.spoofax.eclipse.SpoofaxPlugin;
import org.metaborg.spoofax.eclipse.editor.IEclipseEditor;
import org.metaborg.spoofax.eclipse.editor.IEclipseEditorRegistry;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Injector;

public class TransformMenuContribution extends CompoundContributionItem implements IWorkbenchContribution {
    public static final String transformId = SpoofaxPlugin.id + ".command.transform";
    public static final String actionNameParam = "action-name";

    private static final Logger logger = LoggerFactory.getLogger(TransformMenuContribution.class);

    private final IEclipseResourceService resourceService;
    private final ILanguageIdentifierService languageIdentifier;
    private final IEclipseEditorRegistry latestEditorListener;
    private final IMenuService menuService;

    private IServiceLocator serviceLocator;


    public TransformMenuContribution() {
        final Injector injector = SpoofaxPlugin.injector();
        this.resourceService = injector.getInstance(IEclipseResourceService.class);
        this.languageIdentifier = injector.getInstance(ILanguageIdentifierService.class);
        this.latestEditorListener = injector.getInstance(IEclipseEditorRegistry.class);
        this.menuService = injector.getInstance(IMenuService.class);
    }


    @Override public void initialize(IServiceLocator newServiceLocator) {
        this.serviceLocator = newServiceLocator;
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

        final Iterable<IMenu> menus = menuService.menu(language);
        final Collection<IContributionItem> items = Lists.newLinkedList();
        for(IMenu menu : menus) {
            items.add(createItem(menu));
        }
        return items.toArray(new IContributionItem[0]);
    }

    private IContributionItem createItem(IMenu menu) {
        final MenuManager menuManager = new MenuManager(menu.name());
        for(IMenuItem item : menu.items()) {
            if(item instanceof IMenu) {
                final IContributionItem contribItem = createItem((IMenu) item);
                menuManager.add(contribItem);
            } else if(item instanceof IAction) {
                final IContributionItem contribItem = createItem((IAction) item);
                menuManager.add(contribItem);
            } else if(item instanceof Separator) {
                menuManager.add(new org.eclipse.jface.action.Separator());
            }

        }
        return menuManager;
    }

    private IContributionItem createItem(IAction action) {
        final CommandContributionItemParameter itemParams =
            new CommandContributionItemParameter(serviceLocator, null, transformId, CommandContributionItem.STYLE_PUSH);
        final Map<String, String> parameters = Maps.newHashMap();
        parameters.put(actionNameParam, action.name());
        itemParams.parameters = parameters;
        itemParams.label = action.name();

        return new CommandContributionItem(itemParams);
    }
}
