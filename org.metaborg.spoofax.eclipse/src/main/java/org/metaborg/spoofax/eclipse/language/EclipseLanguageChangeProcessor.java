package org.metaborg.spoofax.eclipse.language;

import java.util.Collection;
import java.util.Set;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.PlatformUI;
import org.metaborg.core.language.ILanguageCache;
import org.metaborg.core.language.ILanguageComponent;
import org.metaborg.core.language.ILanguageIdentifierService;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.language.LanguageFileSelector;
import org.metaborg.core.language.ResourceExtensionFacet;
import org.metaborg.core.language.dialect.IDialectProcessor;
import org.metaborg.core.processing.LanguageChangeProcessor;
import org.metaborg.spoofax.eclipse.editor.IEclipseEditor;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.eclipse.util.EditorMappingUtils;
import org.metaborg.spoofax.eclipse.util.MarkerUtils;
import org.metaborg.spoofax.eclipse.util.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

/**
 * Extends the {@code LanguageChangeProcessor} to include Eclipse-specific operations such as changing editor
 * associations and resource markers.
 */
public class EclipseLanguageChangeProcessor extends LanguageChangeProcessor {
    private static final Logger logger = LoggerFactory.getLogger(EclipseLanguageChangeProcessor.class);

    private final IEclipseResourceService resourceService;
    private final ILanguageIdentifierService languageIdentifier;

    private final IWorkspace workspace;
    private final IEditorRegistry eclipseEditorRegistry;
    private final Display display;


    @Inject public EclipseLanguageChangeProcessor(IEclipseResourceService resourceService,
        ILanguageIdentifierService languageIdentifier, IDialectProcessor dialectProcessor,
        org.metaborg.core.editor.IEditorRegistry editorRegistry, Set<ILanguageCache> languageCaches) {
        super(dialectProcessor, editorRegistry, languageCaches);

        this.resourceService = resourceService;
        this.languageIdentifier = languageIdentifier;

        this.workspace = ResourcesPlugin.getWorkspace();
        this.eclipseEditorRegistry = PlatformUI.getWorkbench().getEditorRegistry();
        this.display = Display.getDefault();
    }


    @Override public void addedComponent(ILanguageComponent component) {
        logger.debug("Running component added job for {}", component);

        final Set<String> extensions = getExtensions(component);
        if(!extensions.isEmpty()) {
            logger.debug("Associating extension(s) {} to Spoofax editor", Joiner.on(", ").join(extensions));
            display.asyncExec(new Runnable() {
                @Override public void run() {
                    EditorMappingUtils.set(eclipseEditorRegistry, IEclipseEditor.id, extensions);
                }
            });
        }

        super.addedComponent(component);
    }

    @Override public void reloadedComponent(ILanguageComponent oldComponent, ILanguageComponent newComponent) {
        logger.debug("Running component reloaded job for {}", newComponent);
        final Set<String> oldExtensions = getExtensions(oldComponent);
        final Set<String> newExtensions = getExtensions(newComponent);
        if(!oldExtensions.isEmpty() || !newExtensions.isEmpty()) {
            final Set<String> removeExtensions = Sets.difference(oldExtensions, newExtensions);
            final Set<String> addExtensions = Sets.difference(newExtensions, oldExtensions);
            if(removeExtensions.size() > 0) {
                logger.debug("Unassociating extension(s) {} from Spoofax editor", Joiner.on(", ")
                    .join(removeExtensions));
            }
            if(addExtensions.size() > 0) {
                logger.debug("Associating extension(s) {} to Spoofax editor", Joiner.on(", ").join(addExtensions));
            }
            display.asyncExec(new Runnable() {
                @Override public void run() {
                    EditorMappingUtils.remove(eclipseEditorRegistry, IEclipseEditor.id, removeExtensions);
                    EditorMappingUtils.set(eclipseEditorRegistry, IEclipseEditor.id, addExtensions);
                }
            });
        }

        super.reloadedComponent(oldComponent, newComponent);
    }

    @Override protected void removedComponent(ILanguageComponent component) {
        logger.debug("Running component removed job for {}", component);

        final Set<String> extensions = getExtensions(component);
        if(!extensions.isEmpty()) {
            logger.debug("Unassociating extension(s) {} from Spoofax editor", Joiner.on(", ").join(extensions));
            display.asyncExec(new Runnable() {
                @Override public void run() {
                    EditorMappingUtils.remove(eclipseEditorRegistry, IEclipseEditor.id, extensions);
                }
            });
        }

        super.removedComponent(component);
    }

    @Override public void removedImpl(ILanguageImpl language) {
        try {
            final Collection<FileObject> resources =
                ResourceUtils.workspaceResources(resourceService,
                    new LanguageFileSelector(languageIdentifier, language), workspace.getRoot());
            final Collection<IResource> eclipseResources = ResourceUtils.toEclipseResources(resourceService, resources);
            logger.debug("Removing markers from {} workspace resources", resources.size());
            for(IResource resource : eclipseResources) {
                try {
                    MarkerUtils.clearAll(resource);
                } catch(CoreException e) {
                    final String message = String.format("Cannot remove markers for resource %s", resource);
                    logger.error(message, e);
                }
            }
        } catch(FileSystemException e) {
            final String message = String.format("Cannot retrieve all workspace resources for %s", language);
            logger.error(message, e);
        }

        super.removedImpl(language);
    }


    private Set<String> getExtensions(ILanguageComponent component) {
        final Set<String> extensions = Sets.newHashSet();
        for(ResourceExtensionFacet facet : component.facets(ResourceExtensionFacet.class)) {
            Iterables.addAll(extensions, facet.extensions());
        }
        return extensions;
    }
}
