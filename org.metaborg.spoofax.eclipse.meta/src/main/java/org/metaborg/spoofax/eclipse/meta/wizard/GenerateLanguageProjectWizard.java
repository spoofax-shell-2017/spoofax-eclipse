package org.metaborg.spoofax.eclipse.meta.wizard;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.metaborg.core.language.LanguageIdentifier;
import org.metaborg.core.language.LanguageVersion;
import org.metaborg.core.project.ProjectException;
import org.metaborg.spoofax.eclipse.meta.SpoofaxMetaPlugin;
import org.metaborg.spoofax.eclipse.meta.nature.SpoofaxMetaNature;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.meta.core.config.ISpoofaxLanguageSpecConfig;
import org.metaborg.spoofax.meta.core.config.ISpoofaxLanguageSpecConfigBuilder;
import org.metaborg.spoofax.meta.core.generator.GeneratorSettings;
import org.metaborg.spoofax.meta.core.generator.eclipse.EclipseLangSpecGenerator;
import org.metaborg.spoofax.meta.core.generator.language.AnalysisType;
import org.metaborg.spoofax.meta.core.generator.language.ContinuousLanguageSpecGenerator;
import org.metaborg.spoofax.meta.core.generator.language.LanguageSpecGenerator;
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpecPaths;
import org.metaborg.spoofax.meta.core.project.SpoofaxLanguageSpecPaths;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.inject.Injector;

public class GenerateLanguageProjectWizard extends Wizard implements INewWizard {
    private static final ILogger logger = LoggerUtils.logger(GenerateLanguageProjectWizard.class);

    private final IEclipseResourceService resourceService;
    private final ISpoofaxLanguageSpecConfigBuilder configBuilder;

    private final GenerateLanguageProjectWizardPage page;

    private volatile IProject lastProject;


    public GenerateLanguageProjectWizard() {
        final Injector injector = SpoofaxMetaPlugin.injector();

        this.resourceService = injector.getInstance(IEclipseResourceService.class);
        this.configBuilder = injector.getInstance(ISpoofaxLanguageSpecConfigBuilder.class);

        this.page = new GenerateLanguageProjectWizardPage();

        addPage(this.page);
        setNeedsProgressMonitor(true);
    }


    @Override public void init(IWorkbench workbench, IStructuredSelection selection) {
    }


    @Override public boolean performFinish() {
        final String groupId = page.groupId();
        final String id = page.id();
        final String versionString = page.version();
        final String name = page.name();
        final String[] extensions = new String[] { "dummy" }; // TODO: get extensions
        final IProject project = page.getProjectHandle();
        final URI projectLocation;
        if(page.useDefaults()) {
            projectLocation = null;
        } else {
            projectLocation = page.getLocationURI();
        }

        final IRunnableWithProgress runnable = new IRunnableWithProgress() {
            public void run(IProgressMonitor monitor) throws InvocationTargetException {
                try {
                    createProject(monitor, groupId, id, versionString, name, extensions, project, projectLocation);
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
            rollback();
            return false;
        } catch(InvocationTargetException e) {
            final Throwable t = e.getTargetException();
            logger.error("Generating project failed", t);
            MessageDialog.openError(getShell(), "Error: " + t.getClass().getName(), t.getMessage());
            rollback();
            return false;
        }
        return true;
    }

    private void createProject(IProgressMonitor monitor, String groupId, String id, String versionString, String name,
        String[] extensions, IProject eclipseProject, URI projectLocation)
            throws ProjectException, IOException, CoreException {
        if(projectLocation != null) {
            IWorkspace workspace = ResourcesPlugin.getWorkspace();
            final IProjectDescription description = workspace.newProjectDescription(eclipseProject.getName());
            description.setLocationURI(projectLocation);
            eclipseProject.create(description, monitor);
        } else {
            eclipseProject.create(monitor);
        }
        lastProject = eclipseProject;
        eclipseProject.open(monitor);

        final FileObject location = resourceService.resolve(eclipseProject);

        final LanguageVersion version = LanguageVersion.parse(versionString);
        final LanguageIdentifier identifier = new LanguageIdentifier(groupId, id, version);
        final ISpoofaxLanguageSpecConfig config =
            configBuilder.withIdentifier(identifier).withName(name).build(location);
        final ISpoofaxLanguageSpecPaths paths = new SpoofaxLanguageSpecPaths(location, config);
        final GeneratorSettings generatorSettings = new GeneratorSettings(config, paths);

        final LanguageSpecGenerator newGenerator =
            new LanguageSpecGenerator(generatorSettings, extensions, AnalysisType.NaBL_TS);
        newGenerator.generateAll();
        final ContinuousLanguageSpecGenerator generator = new ContinuousLanguageSpecGenerator(generatorSettings);
        generator.generateAll();
        final EclipseLangSpecGenerator eclipseGenerator = new EclipseLangSpecGenerator(generatorSettings);
        eclipseGenerator.generateAll();

        SpoofaxMetaNature.add(eclipseProject, monitor);
    }

    private void rollback() {
        if(lastProject != null) {
            try {
                lastProject.delete(true, null);
            } catch(CoreException e) {
                logger.error("Cannot rollback project creation, {} cannot be deleted", e, lastProject);
            }
        }
        lastProject = null;
    }
}
