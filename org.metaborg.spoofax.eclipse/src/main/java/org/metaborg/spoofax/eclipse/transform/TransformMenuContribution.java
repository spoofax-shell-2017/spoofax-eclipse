package org.metaborg.spoofax.eclipse.transform;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.services.IServiceLocator;
import org.metaborg.core.MetaborgRuntimeException;
import org.metaborg.core.language.ILanguageIdentifierService;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.language.LanguageIdentifier;
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

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Injector;

public class TransformMenuContribution extends CompoundContributionItem implements IWorkbenchContribution {
    public static final String transformId = SpoofaxPlugin.id + ".command.transform";
    public static final String languageIdParam = "language-id";
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

        final Iterable<IMenuItem> menuItems = menuService.menuItems(language);
        final Collection<IContributionItem> items = Lists.newLinkedList();
        for(IMenuItem menuItem : menuItems) {
            items.add(createItem(menuItem, language));
        }
        return items.toArray(new IContributionItem[0]);
    }

    private IContributionItem createItem(IMenuItem item, ILanguageImpl language) {
        if(item instanceof IMenu) {
            return createMenu((IMenu) item, language);
        } else if(item instanceof IAction) {
            return createAction((IAction) item, language);
        } else if(item instanceof Separator) {
            return new org.eclipse.jface.action.Separator();
        } else {
            throw new MetaborgRuntimeException("Unhandled menu item: " + item.getClass());
        }
    }

    private IContributionItem createMenu(IMenu menu, ILanguageImpl language) {
        final MenuManager menuManager = new MenuManager(menu.name());
        for(IMenuItem item : menu.items()) {
            final IContributionItem contribItem = createItem(item, language);
            menuManager.add(contribItem);
        }
        return menuManager;
    }

    private IContributionItem createAction(IAction action, ILanguageImpl language) {
        final CommandContributionItemParameter itemParams =
            new CommandContributionItemParameter(serviceLocator, null, transformId, CommandContributionItem.STYLE_PUSH);
        final Map<String, String> parameters = Maps.newHashMap();
        parameters.put(languageIdParam, toProperty(language.id()));
        parameters.put(actionNameParam, toProperty(action));
        itemParams.parameters = parameters;
        itemParams.label = action.name();

        return new CommandContributionItem(itemParams);
    }


    private static String toProperty(LanguageIdentifier identifier) {
        return identifier.toString();
    }

    private static String toProperty(IAction action) {
        return Joiner.on(" ---> ").join(action.goal().names);
    }


    public static LanguageIdentifier fromLanguageIdProperty(ExecutionEvent event) {
        return LanguageIdentifier.parse(event.getParameter(languageIdParam));
    }

    public static List<String> fromActionNameProperty(ExecutionEvent event) {
        return Splitter.on(" ---> ").splitToList(event.getParameter(actionNameParam));
    }
}
