package org.metaborg.spoofax.eclipse;

import org.apache.commons.vfs2.FileSystemManager;
import org.metaborg.core.MetaborgModule;
import org.metaborg.core.editor.IEditorRegistry;
import org.metaborg.core.processing.ILanguageChangeProcessor;
import org.metaborg.core.processing.IProcessor;
import org.metaborg.core.project.IProjectService;
import org.metaborg.core.resource.IResourceService;
import org.metaborg.spoofax.core.SpoofaxModule;
import org.metaborg.spoofax.core.processing.ISpoofaxProcessor;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnitUpdate;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxTransformUnit;
import org.metaborg.spoofax.eclipse.editor.IEclipseEditorRegistry;
import org.metaborg.spoofax.eclipse.editor.IEclipseEditorRegistryInternal;
import org.metaborg.spoofax.eclipse.editor.SpoofaxEditorRegistry;
import org.metaborg.spoofax.eclipse.job.GlobalSchedulingRules;
import org.metaborg.spoofax.eclipse.language.EclipseLanguageChangeProcessor;
import org.metaborg.spoofax.eclipse.language.LanguageLoader;
import org.metaborg.spoofax.eclipse.processing.SpoofaxProcessor;
import org.metaborg.spoofax.eclipse.project.EclipseProjectService;
import org.metaborg.spoofax.eclipse.project.IEclipseProjectService;
import org.metaborg.spoofax.eclipse.resource.EclipseFileSystemManagerProvider;
import org.metaborg.spoofax.eclipse.resource.EclipseResourceService;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.spoofax.interpreter.terms.IStrategoTerm;

import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;

public class SpoofaxEclipseModule extends SpoofaxModule {
    public SpoofaxEclipseModule() {
        super(SpoofaxEclipseModule.class.getClassLoader());
    }

    public SpoofaxEclipseModule(ClassLoader resourceClassLoader) {
        super(resourceClassLoader);
    }


    @Override protected void configure() {
        super.configure();

        bind(GlobalSchedulingRules.class).in(Singleton.class);
        bind(EclipseLanguageChangeProcessor.class).in(Singleton.class);
        bind(LanguageLoader.class).in(Singleton.class);
        bind(SpoofaxPreferences.class).in(Singleton.class);
    }


    /**
     * Overrides {@link MetaborgModule#bindResource()} to provide an Eclipse resource manager and filesystem
     * implementation.
     */
    @Override protected void bindResource() {
        bind(EclipseResourceService.class).in(Singleton.class);
        bind(IResourceService.class).to(EclipseResourceService.class);
        bind(IEclipseResourceService.class).to(EclipseResourceService.class);

        bind(FileSystemManager.class).toProvider(EclipseFileSystemManagerProvider.class).in(Singleton.class);
    }

    /**
     * Overrides {@link MetaborgModule#bindProject()} for non-dummy implementation of project service.
     */
    @Override protected void bindProject() {
        bind(EclipseProjectService.class).in(Singleton.class);
        bind(IProjectService.class).to(EclipseProjectService.class);
        bind(IEclipseProjectService.class).to(EclipseProjectService.class);
    }

    /**
     * Overrides {@link SpoofaxModule#bindProcessor()} with an Eclipse-specific processor.
     */
    @Override protected void bindProcessor() {
        bind(SpoofaxProcessor.class).in(Singleton.class);
        bind(ISpoofaxProcessor.class).to(SpoofaxProcessor.class);
        bind(IProcessor.class).to(SpoofaxProcessor.class);
        bind(
            new TypeLiteral<IProcessor<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit, ISpoofaxAnalyzeUnitUpdate, ISpoofaxTransformUnit<?>>>() {})
                .to(SpoofaxProcessor.class);
        bind(new TypeLiteral<IProcessor<?, ?, ?, ?>>() {}).to(SpoofaxProcessor.class);
    }

    /**
     * Overrides {@link SpoofaxModule#bindLanguageChangeProcessing()} with an Eclipse-specific language change
     * processor.
     */
    @Override protected void bindLanguageChangeProcessing() {
        bind(ILanguageChangeProcessor.class).to(EclipseLanguageChangeProcessor.class).in(Singleton.class);
    }

    /**
     * Overrides {@link MetaborgModule#bindEditor()} to provide an Eclipse editor registry implementation.
     */
    @Override protected void bindEditor() {
        bind(SpoofaxEditorRegistry.class).in(Singleton.class);
        bind(IEditorRegistry.class).to(SpoofaxEditorRegistry.class);
        bind(new TypeLiteral<IEclipseEditorRegistry<IStrategoTerm>>() {}).to(SpoofaxEditorRegistry.class);
        bind(IEclipseEditorRegistry.class).to(SpoofaxEditorRegistry.class);
        bind(IEclipseEditorRegistryInternal.class).to(SpoofaxEditorRegistry.class);
    }
}
