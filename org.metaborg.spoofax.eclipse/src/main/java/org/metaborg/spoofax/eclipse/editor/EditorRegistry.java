package org.metaborg.spoofax.eclipse.editor;

import java.util.Collection;
import java.util.Set;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.contexts.IContextService;
import org.metaborg.core.editor.IEditor;
import org.metaborg.core.project.IProject;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.eclipse.util.EditorUtils;
import org.metaborg.spoofax.eclipse.util.Nullable;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

/**
 * Keeps track of all editors, which one is currently active, and which one was active previously.
 */
public class EditorRegistry<F>
    implements IWindowListener, IPartListener2, IEclipseEditorRegistry<F>, IEclipseEditorRegistryInternal {
    private static final ILogger logger = LoggerUtils.logger(EditorRegistry.class);

    public static final String contextId = SpoofaxEditor.id + ".context";

    private final IEclipseResourceService resourceService;

    private IContextService contextService;
    private IContextActivation contextActivation;

    private volatile Set<IEclipseEditor<F>> editors = Sets.newConcurrentHashSet();
    private volatile IEclipseEditor<F> currentActive;
    private volatile IEclipseEditor<F> previousActive;


    @Inject public EditorRegistry(IEclipseResourceService resourceService) {
        this.resourceService = resourceService;
    }


    @SuppressWarnings("cast") @Override public void register() {
        final IWorkbench workbench = PlatformUI.getWorkbench();
        contextService = (IContextService) workbench.getService(IContextService.class);

        Display.getDefault().asyncExec(new Runnable() {
            @Override public void run() {
                final IWorkbenchWindow[] windows = workbench.getWorkbenchWindows();
                for(IWorkbenchWindow window : windows) {
                    windowOpened(window);
                    for(IWorkbenchPage page : window.getPages()) {
                        for(IEditorReference editorRef : page.getEditorReferences()) {
                            final IEclipseEditor<F> editor = get(editorRef);
                            if(editor != null) {
                                add(editor);
                            }
                        }
                    }
                }
                final IWorkbenchWindow activeWindow = workbench.getActiveWorkbenchWindow();
                if(activeWindow != null) {
                    final IWorkbenchPage activePage = activeWindow.getActivePage();
                    if(activePage != null) {
                        final IEditorPart activeEditorPart = activePage.getActiveEditor();
                        if(activeEditorPart != null) {
                            final IEclipseEditor<F> editor = get(activeEditorPart);
                            if(editor != null) {
                                activate(editor);
                            }
                        }
                    }
                }
                workbench.addWindowListener(EditorRegistry.this);
            }
        });
    }


    @Override public Iterable<IEditor> openEditors() {
        final Collection<IEditor> openEditors = Lists.newArrayListWithCapacity(editors.size());
        for(IEclipseEditor<F> editor : editors) {
            openEditors.add(editor);
        }
        return openEditors;
    }

    @Override public void open(FileObject resource, IProject project) {
        final IResource eclipseResource = resourceService.unresolve(resource);
        if(eclipseResource instanceof IFile) {
            final IFile file = (IFile) eclipseResource;
            EditorUtils.open(file);
        }
    }


    @Override public Iterable<IEclipseEditor<F>> openEclipseEditors() {
        return editors;
    }


    @Override public @Nullable IEclipseEditor<F> currentEditor() {
        return currentActive;
    }

    @Override public @Nullable IEclipseEditor<F> previousEditor() {
        return previousActive;
    }


    private IEclipseEditor<F> get(IWorkbenchPartReference part) {
        return get(part.getPart(false));
    }

    @SuppressWarnings("unchecked") private IEclipseEditor<F> get(IWorkbenchPart part) {
        if(part instanceof IEclipseEditor) {
            return (IEclipseEditor<F>) part;
        }
        return null;
    }

    private boolean isEditor(IWorkbenchPartReference part) {
        return part instanceof IEditorReference;
    }

    private void setCurrent(IEclipseEditor<F> editor) {
        currentActive = editor;
        if(contextActivation == null) {
            contextActivation = contextService.activateContext(contextId);
        }
    }

    private void unsetCurrent() {
        currentActive = null;
        if(contextActivation != null) {
            contextService.deactivateContext(contextActivation);
            contextActivation = null;
        }
    }

    private void add(IEclipseEditor<F> editor) {
        logger.trace("Adding {}", editor);
        editors.add(editor);
    }

    private void remove(IEclipseEditor<F> editor) {
        logger.trace("Removing {}", editor);
        editors.remove(editor);
        if(currentActive == editor) {
            logger.trace("Unsetting active (by remove) {}", editor);
            unsetCurrent();
        }
        if(previousActive == editor) {
            logger.trace("Unsetting latest (by remove) {}", editor);
            previousActive = null;
        }
    }

    private void activate(IEclipseEditor<F> editor) {
        logger.trace("Setting active {}", editor);
        setCurrent(editor);
        logger.trace("Setting latest {}", editor);
        previousActive = editor;
    }

    private void activateOther() {
        logger.trace("Unsetting active (by activate other) {}", currentActive);
        unsetCurrent();
        logger.trace("Unsetting latest (by activate other) {}", previousActive);
        previousActive = null;
    }

    private void deactivate(IEclipseEditor<F> editor) {
        if(currentActive == editor) {
            logger.trace("Unsetting active (by deactivate) {}", currentActive);
            unsetCurrent();
        }
    }


    @Override public void windowActivated(IWorkbenchWindow window) {
        final IEclipseEditor<F> editor = get(window.getPartService().getActivePart());
        if(editor == null) {
            return;
        }
        activate(editor);
    }

    @Override public void windowDeactivated(IWorkbenchWindow window) {
        final IEclipseEditor<F> editor = get(window.getPartService().getActivePart());
        if(editor == null) {
            return;
        }
        deactivate(editor);
    }

    @Override public void windowOpened(IWorkbenchWindow window) {
        window.getPartService().addPartListener(this);
    }

    @Override public void windowClosed(IWorkbenchWindow window) {
        window.getPartService().removePartListener(this);
    }


    @Override public void partActivated(IWorkbenchPartReference partRef) {
        final IEclipseEditor<F> editor = get(partRef);
        if(editor != null) {
            activate(editor);
        } else if(isEditor(partRef)) {
            activateOther();
        }
    }

    @Override public void partBroughtToTop(IWorkbenchPartReference partRef) {

    }

    @Override public void partClosed(IWorkbenchPartReference partRef) {
        final IEclipseEditor<F> editor = get(partRef);
        if(editor != null) {
            remove(editor);
        }
    }

    @Override public void partDeactivated(IWorkbenchPartReference partRef) {
        final IEclipseEditor<F> editor = get(partRef);
        if(editor != null) {
            deactivate(editor);
        }
    }

    @Override public void partOpened(IWorkbenchPartReference partRef) {
        final IEclipseEditor<F> editor = get(partRef);
        if(editor != null) {
            add(editor);
        }
    }

    @Override public void partHidden(IWorkbenchPartReference partRef) {

    }

    @Override public void partVisible(IWorkbenchPartReference partRef) {

    }

    @Override public void partInputChanged(IWorkbenchPartReference partRef) {

    }
}
