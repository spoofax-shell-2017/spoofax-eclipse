package org.metaborg.spoofax.eclipse.transform;

import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang3.SerializationUtils;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.services.IServiceLocator;
import org.metaborg.core.MetaborgRuntimeException;
import org.metaborg.core.action.ITransformGoal;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.language.LanguageIdentifier;
import org.metaborg.core.menu.IMenu;
import org.metaborg.core.menu.IMenuAction;
import org.metaborg.core.menu.IMenuItem;
import org.metaborg.core.menu.IMenuService;
import org.metaborg.core.menu.Separator;
import org.metaborg.spoofax.eclipse.SpoofaxPlugin;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.BaseEncoding;
import com.google.inject.Injector;

public abstract class MenuContribution extends CompoundContributionItem implements IWorkbenchContribution {
    public static final String transformId = SpoofaxPlugin.id + ".command.transform";
    public static final String languageIdParam = "language-id";
    public static final String actionNameParam = "action-name";
    public static final String hasOpenEditorParam = "has-open-editor";

    private final IMenuService menuService;

    private IServiceLocator serviceLocator;


    public MenuContribution() {
        final Injector injector = SpoofaxPlugin.injector();
        this.menuService = injector.getInstance(IMenuService.class);
    }


    @Override public void initialize(IServiceLocator newServiceLocator) {
        this.serviceLocator = newServiceLocator;
    }

    protected abstract IContributionItem[] getContributionItems();

    protected IContributionItem[] getContributionItems(ILanguageImpl language, boolean hasOpenEditor) {
        final Iterable<IMenuItem> menuItems = menuService.menuItems(language);
        final Collection<IContributionItem> items = Lists.newLinkedList();
        for(IMenuItem menuItem : menuItems) {
            items.add(createItem(menuItem, language, hasOpenEditor));
        }
        return items.toArray(new IContributionItem[items.size()]);
    }


    private IContributionItem createItem(IMenuItem item, ILanguageImpl language, boolean hasOpenEditor) {
        if(item instanceof IMenu) {
            return createMenu((IMenu) item, language, hasOpenEditor);
        } else if(item instanceof IMenuAction) {
            return createAction((IMenuAction) item, language, hasOpenEditor);
        } else if(item instanceof Separator) {
            return new org.eclipse.jface.action.Separator();
        } else {
            throw new MetaborgRuntimeException("Unhandled menu item: " + item.getClass());
        }
    }

    private IContributionItem createMenu(IMenu menu, ILanguageImpl language, boolean hasOpenEditor) {
        final MenuManager menuManager = new MenuManager(menu.name());
        for(IMenuItem item : menu.items()) {
            final IContributionItem contribItem = createItem(item, language, hasOpenEditor);
            menuManager.add(contribItem);
        }
        return menuManager;
    }

    private IContributionItem createAction(IMenuAction action, ILanguageImpl language, boolean hasOpenEditor) {
        final CommandContributionItemParameter itemParams =
            new CommandContributionItemParameter(serviceLocator, null, transformId, CommandContributionItem.STYLE_PUSH);
        final Map<String, String> parameters = Maps.newHashMap();
        parameters.put(languageIdParam, language.id().toString());
        final ITransformGoal goal = action.action().goal();
        parameters.put(actionNameParam, BaseEncoding.base64().encode(SerializationUtils.serialize(goal)));
        parameters.put(hasOpenEditorParam, Boolean.toString(hasOpenEditor));
        itemParams.parameters = parameters;
        itemParams.label = action.name();

        return new CommandContributionItem(itemParams);
    }


    public static LanguageIdentifier toLanguageId(ExecutionEvent event) {
        return LanguageIdentifier.parse(event.getParameter(languageIdParam));
    }

    public static ITransformGoal toGoal(ExecutionEvent event) {
        return SerializationUtils.deserialize(BaseEncoding.base64().decode(event.getParameter(actionNameParam)));
    }

    public static boolean toHasOpenEditor(ExecutionEvent event) {
        return Boolean.parseBoolean(event.getParameter(hasOpenEditorParam));
    }
}
