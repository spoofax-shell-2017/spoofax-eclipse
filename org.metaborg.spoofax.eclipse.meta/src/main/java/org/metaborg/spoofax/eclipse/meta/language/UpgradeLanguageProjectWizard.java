package org.metaborg.spoofax.eclipse.meta.language;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.vfs2.AllFileSelector;
import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.metaborg.spoofax.core.esv.ESVReader;
import org.metaborg.spoofax.core.terms.ITermFactoryService;
import org.metaborg.spoofax.eclipse.meta.nature.SpoofaxMetaNature;
import org.metaborg.spoofax.eclipse.nature.SpoofaxNature;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.eclipse.util.BuilderUtils;
import org.metaborg.spoofax.eclipse.util.NatureUtils;
import org.metaborg.spoofax.eclipse.util.StatusUtils;
import org.metaborg.spoofax.generator.NewProjectGenerator;
import org.metaborg.spoofax.generator.ProjectGenerator;
import org.metaborg.spoofax.generator.project.ProjectSettings;
import org.metaborg.util.resource.ContainsFileSelector;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.ParseError;
import org.spoofax.terms.io.binary.TermReader;

public class UpgradeLanguageProjectWizard extends Wizard {
    private final IProject eclipseProject;
    private final FileObject project;
    private final File projectFile;
    private final UpgradeLanguageProjectWizardPage page;


    public UpgradeLanguageProjectWizard(IEclipseResourceService resourceService,
        ITermFactoryService termFactoryService, IProject eclipseProject) {
        this.eclipseProject = eclipseProject;
        this.project = resourceService.resolve(eclipseProject);
        this.projectFile = resourceService.localPath(project);

        String name;
        String id;
        try {
            final FileObject[] files = project.findFiles(new ContainsFileSelector("packed.esv"));
            if(files.length == 0) {
                name = "";
                id = "";
            } else {
                final FileObject esvFile = files[0];
                final TermReader reader =
                    new TermReader(termFactoryService.getGeneric().getFactoryWithStorageType(IStrategoTerm.MUTABLE));
                final IStrategoTerm term = reader.parseFromStream(esvFile.getContent().getInputStream());
                if(term.getTermType() != IStrategoTerm.APPL) {
                    throw new IllegalStateException("Packed ESV file does not contain a valid ESV term.");
                }
                final IStrategoAppl esvTerm = (IStrategoAppl) term;

                name = ESVReader.getProperty(esvTerm, "LanguageName");
                id = ESVReader.getProperty(esvTerm, "LanguageId");
            }
        } catch(ParseError | IOException e) {
            name = "";
            id = "";
        }

        this.page = new UpgradeLanguageProjectWizardPage(name, id);

        setNeedsProgressMonitor(true);
    }


    @Override public void addPages() {
        addPage(page);
    }

    @Override public boolean performFinish() {
        final String languageName = page.inputLanguageName.getText();
        final String packageName = page.inputPackageName.getText();

        final IRunnableWithProgress runnable = new IRunnableWithProgress() {
            public void run(IProgressMonitor monitor) throws InvocationTargetException {
                try {
                    doUpgrade(monitor, languageName, packageName);
                } catch(Exception e) {
                    throw new InvocationTargetException(e);
                } finally {
                    monitor.done();
                }
            }
        };

        try {
            getContainer().run(true, false, runnable);
        } catch(InterruptedException e) {
            return false;
        } catch(InvocationTargetException e) {
            final Throwable t = e.getTargetException();
            MessageDialog.openError(getShell(), "Error: " + t.getClass().getName(), t.getMessage());
            return false;
        }
        return true;
    }

    private void doUpgrade(IProgressMonitor monitor, final String name, final String id) throws Exception {
        final IWorkspaceRunnable upgradeRunnable = new IWorkspaceRunnable() {
            @Override public void run(IProgressMonitor workspaceMonitor) throws CoreException {
                try {
                    workspaceMonitor.beginTask("Upgrading language project", 3);
                    deleteUnused(name, id);
                    workspaceMonitor.worked(1);
                    upgradeProject();
                    workspaceMonitor.worked(1);
                    generateFiles(name, id);
                    workspaceMonitor.worked(1);
                } catch(CoreException e) {
                    throw e;
                } catch(Exception e) {
                    throw new CoreException(StatusUtils.error(e));
                }
            }
        };
        ResourcesPlugin.getWorkspace().run(upgradeRunnable, ResourcesPlugin.getWorkspace().getRoot(),
            IWorkspace.AVOID_UPDATE, monitor);
    }

    private void deleteUnused(String name, String id) throws Exception {
        // Delete IMP classes
        final String impClassesLoc = id.replace(".", File.separator);
        final FileObject impClassesDir = project.resolveFile("editor/java/" + impClassesLoc);
        if(impClassesDir.exists()) {
            impClassesDir.resolveFile("Activator.java").delete();
            final String className = name.replaceAll("[^a-zA-Z0-9\\_\\$]", "");
            impClassesDir.resolveFile(className + "ParseController.java").delete();
            impClassesDir.resolveFile(className + "ParseControllerGenerated.java").delete();
            impClassesDir.resolveFile(className + "Validator.java").delete();
            impClassesDir.refresh();
            if(impClassesDir.getChildren().length == 0) {
                impClassesDir.delete();
            }
        }

        // Delete Ant build
        final FileObject antBuilderDir = project.resolveFile(".externalToolBuilders");
        if(antBuilderDir.exists()) {
            antBuilderDir.resolveFile(name + " build.main.xml.launch").delete();
            antBuilderDir.resolveFile(name + " clean-project.xml.launch").delete();
            antBuilderDir.refresh();
            if(antBuilderDir.getChildren().length == 0) {
                antBuilderDir.delete();
            }
        }
        project.resolveFile("build.main.xml").delete();
        project.resolveFile("build.generated.xml").delete();

        // Delete Eclipse files
        project.resolveFile(".settings").delete(new AllFileSelector());
        project.resolveFile("META-INF").delete(new AllFileSelector());
        project.resolveFile("build.properties").delete();
        project.resolveFile("plugin.xml").delete();

        // Delete other files
        project.resolveFile(".cache").delete(new AllFileSelector());
        final FileObject libDir = project.resolveFile("lib");
        if(libDir.exists()) {
            libDir.resolveFile("runtime").delete(new AllFileSelector());
            libDir.resolveFile("editor-common.generated.str").delete();
            libDir.resolveFile("refactor-common.generated.str").delete();
            libDir.refresh();
            if(libDir.getChildren().length == 0) {
                libDir.delete();
            }
        }
        project.resolveFile("src-gen").delete(new AllFileSelector());
        project.resolveFile("target").delete(new AllFileSelector());
        project.resolveFile("utils").delete(new AllFileSelector());
        project.resolveFile(".gitignore").delete();
        project.resolveFile("pom.xml").delete();
    }

    private void upgradeProject() throws Exception {
        BuilderUtils.removeFrom("org.eclipse.ui.externaltools.ExternalToolBuilder", eclipseProject);
        BuilderUtils.removeFrom("org.eclipse.pde.ManifestBuilder", eclipseProject);
        BuilderUtils.removeFrom("org.eclipse.pde.SchemaBuilder", eclipseProject);

        NatureUtils.removeFrom("org.strategoxt.imp.metatooling.nature", eclipseProject);
        NatureUtils.removeFrom("org.eclipse.pde.PluginNature", eclipseProject);

        NatureUtils.addTo(SpoofaxNature.id, eclipseProject);
        NatureUtils.addTo(SpoofaxMetaNature.id, eclipseProject);
        NatureUtils.addTo("org.eclipse.m2e.core.maven2Nature", eclipseProject);
    }

    private void generateFiles(String name, String id) throws Exception {
        final ProjectSettings settings = new ProjectSettings(name, projectFile);
        settings.setId(id);
        final NewProjectGenerator newProjectGenerator = new NewProjectGenerator(settings, new String[] { "dummy" });
        newProjectGenerator.generateIgnoreFile();
        newProjectGenerator.generatePOM();
        final ProjectGenerator projectGenerator = new ProjectGenerator(settings);
        projectGenerator.generateAll();
    }
}
