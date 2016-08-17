package org.metaborg.spoofax.eclipse.meta.wizard;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.metaborg.core.config.ConfigException;
import org.metaborg.core.language.LanguageIdentifier;
import org.metaborg.core.project.ProjectException;
import org.metaborg.spoofax.eclipse.meta.SpoofaxMetaPlugin;
import org.metaborg.spoofax.eclipse.util.Nullable;
import org.metaborg.spoofax.meta.core.config.ISpoofaxLanguageSpecConfig;
import org.metaborg.spoofax.meta.core.generator.general.AnalysisType;
import org.metaborg.spoofax.meta.core.generator.general.SyntaxType;
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpec;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.inject.Injector;

public class CreateLangSpecWizard extends Wizard implements INewWizard {
    private static final ILogger logger = LoggerUtils.logger(CreateLangSpecWizard.class);

    private final ProjectGenerator projectGenerator;

    private final CreateLangSpecWizardPage page;


    public CreateLangSpecWizard() {
        final Injector injector = SpoofaxMetaPlugin.injector();
        this.projectGenerator = injector.getInstance(ProjectGenerator.class);

        this.page = new CreateLangSpecWizardPage();

        addPage(this.page);
        setNeedsProgressMonitor(true);
    }


    @Override public void init(IWorkbench workbench, IStructuredSelection selection) {
    }


    @Override public boolean performFinish() {
        final @Nullable IPath basePath;
        if(page.useDefaults()) {
            basePath = null;
        } else {
            basePath = page.getLocationPath();
        }

        final String languageName = page.languageName();
        final LanguageIdentifier languageId = page.languageIdentifier();
        final Collection<String> extensions = page.extensions();
        final SyntaxType syntaxType = page.syntaxType();
        final AnalysisType analysisType = page.analysisType();
        final boolean generateExampleProject = page.generateExampleProject();
        final boolean generateTestProject = page.generateTestProject();
        final boolean generateEclipsePluginProject = page.generateEclipsePluginProject();
        final boolean generateEclipseFeatureProject = page.generateEclipseFeatureProject();
        final boolean generateEclipseUpdatesiteProject = page.generateEclipseUpdatesiteProject();

        final IRunnableWithProgress runnable = new IRunnableWithProgress() {
            public void run(IProgressMonitor monitor) throws InvocationTargetException {
                try {
                    createAll(monitor, languageId, languageName, extensions, syntaxType, analysisType,
                        generateExampleProject, generateTestProject, generateEclipsePluginProject,
                        generateEclipseFeatureProject, generateEclipseUpdatesiteProject, basePath);
                } catch(Throwable e) {
                    throw new InvocationTargetException(e);
                } finally {
                    monitor.done();
                }
            }
        };

        try {
            getContainer().run(true, true, runnable);
        } catch(InterruptedException e) {
            return false;
        } catch(InvocationTargetException e) {
            final Throwable t = e.getTargetException();
            logger.error("Generating project failed", t);
            MessageDialog.openError(getShell(), "Error: " + t.getClass().getName(), t.getMessage());
            return false;
        }
        return true;
    }

    private void createAll(IProgressMonitor rootMonitor, LanguageIdentifier languageId, String languageName,
        Collection<String> extensions, SyntaxType syntaxType, AnalysisType analysisType, boolean generateExampleProject,
        boolean generateTestProject, boolean generateEclipsePluginProject, boolean generateEclipseFeatureProject,
        boolean generateEclipseUpdatesiteProject, @Nullable IPath basePath)
        throws ProjectException, IOException, CoreException, ConfigException, OperationCanceledException {
        final SubMonitor monitor = SubMonitor.convert(rootMonitor, "Generating language projects",
            100 + (generateExampleProject ? 1 : 0) + (generateTestProject ? 1 : 0)
                + (generateEclipsePluginProject ? 1 : 0) + (generateEclipseFeatureProject ? 1 : 0)
                + (generateEclipseUpdatesiteProject ? 1 : 0));

        final ISpoofaxLanguageSpec langSpec = projectGenerator.createLangSpecProject(languageId, languageName,
            extensions, syntaxType, analysisType, basePath, monitor.newChild(100));
        final ISpoofaxLanguageSpecConfig config = langSpec.config();
        if(generateExampleProject) {
            projectGenerator.createExampleProject(config, null, basePath, monitor.newChild(1));
        }
        if(generateTestProject) {
            projectGenerator.createTestProject(config, null, basePath, monitor.newChild(1));
        }
        if(generateEclipsePluginProject) {
            projectGenerator.createEclipsePluginProject(config, null, basePath, monitor.newChild(1));
        }
        if(generateEclipseFeatureProject) {
            projectGenerator.createEclipseFeatureProject(config, null, basePath, monitor.newChild(1));
        }
        if(generateEclipseUpdatesiteProject) {
            projectGenerator.createEclipseSiteProject(config, null, basePath, monitor.newChild(1));
        }

        monitor.done();
    }
}
