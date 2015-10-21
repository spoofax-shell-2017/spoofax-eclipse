package org.metaborg.spoofax.eclipse.meta.project;

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
import org.metaborg.core.project.settings.IProjectSettings;
import org.metaborg.core.project.settings.ProjectSettings;
import org.metaborg.spoofax.core.project.settings.SpoofaxProjectSettings;
import org.metaborg.spoofax.eclipse.meta.SpoofaxMetaPlugin;
import org.metaborg.spoofax.eclipse.meta.nature.SpoofaxMetaNature;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.generator.eclipse.language.EclipseProjectGenerator;
import org.metaborg.spoofax.generator.language.NewProjectGenerator;
import org.metaborg.spoofax.generator.language.ProjectGenerator;
import org.metaborg.spoofax.generator.project.GeneratorProjectSettings;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.inject.Injector;

public class GenerateLanguageProjectWizard extends Wizard implements INewWizard {
    private static final ILogger logger = LoggerUtils.logger(GenerateLanguageProjectWizard.class);

    private final IEclipseResourceService resourceService;

    private final GenerateLanguageProjectWizardPage page;

    private volatile IProject lastProject;


    public GenerateLanguageProjectWizard() {
        final Injector injector = SpoofaxMetaPlugin.injector();

        this.resourceService = injector.getInstance(IEclipseResourceService.class);

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
        String[] extensions, IProject project, URI projectLocation) throws ProjectException, IOException, CoreException {
        if(projectLocation != null) {
            IWorkspace workspace = ResourcesPlugin.getWorkspace();
            final IProjectDescription description = workspace.newProjectDescription(project.getName());
            description.setLocationURI(projectLocation);
            project.create(description, monitor);
        } else {
            project.create(monitor);
        }
        lastProject = project;
        project.open(monitor);

        final FileObject location = resourceService.resolve(project);

        final LanguageVersion version = LanguageVersion.parse(versionString);
        final LanguageIdentifier identifier = new LanguageIdentifier(groupId, id, version);
        final IProjectSettings settings = new ProjectSettings(identifier, name);
        final SpoofaxProjectSettings spoofaxSettings = new SpoofaxProjectSettings(settings, location);
        final GeneratorProjectSettings generatorSettings = new GeneratorProjectSettings(spoofaxSettings);

        final NewProjectGenerator newGenerator = new NewProjectGenerator(generatorSettings, extensions);
        newGenerator.generateAll();
        final ProjectGenerator generator = new ProjectGenerator(generatorSettings);
        generator.generateAll();
        final EclipseProjectGenerator eclipseGenerator = new EclipseProjectGenerator(generatorSettings);
        eclipseGenerator.generateAll();

        SpoofaxMetaNature.add(project, monitor);
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
