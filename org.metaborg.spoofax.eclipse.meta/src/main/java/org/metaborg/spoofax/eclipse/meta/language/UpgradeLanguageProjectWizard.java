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
    private final IEclipseResourceService resourceService;
    private final IProject eclipseProject;
    private final FileObject project;
    private final UpgradeLanguageProjectWizardPage page;


    public UpgradeLanguageProjectWizard(IEclipseResourceService resourceService,
        ITermFactoryService termFactoryService, IProject eclipseProject) {
        this.resourceService = resourceService;
        this.eclipseProject = eclipseProject;
        this.project = resourceService.resolve(eclipseProject);

        String groupId = "";
        String id = "";
        String version = "";
        String name = "";
        try {
            final FileObject[] files = project.findFiles(new ContainsFileSelector("packed.esv"));
            if(files.length > 0) {
                final FileObject esvFile = files[0];
                final TermReader reader =
                    new TermReader(termFactoryService.getGeneric().getFactoryWithStorageType(IStrategoTerm.MUTABLE));
                final IStrategoTerm term = reader.parseFromStream(esvFile.getContent().getInputStream());
                if(term.getTermType() != IStrategoTerm.APPL) {
                    throw new IllegalStateException("Packed ESV file does not contain a valid ESV term.");
                }
                final IStrategoAppl esvTerm = (IStrategoAppl) term;

                groupId = ESVReader.getProperty(esvTerm, "LanguageGroupId");
                id = ESVReader.getProperty(esvTerm, "LanguageId");
                version = ESVReader.getProperty(esvTerm, "LanguageVersion");
                name = ESVReader.getProperty(esvTerm, "LanguageName");
            }
        } catch(ParseError | IOException e) {

        }

        groupId = groupId == null ? "" : groupId;
        id = id == null ? "" : id;
        version = version == null ? "" : version;
        name = name == null ? "" : name;

        this.page = new UpgradeLanguageProjectWizardPage(groupId, id, version, name);

        setNeedsProgressMonitor(true);
    }


    @Override public void addPages() {
        addPage(page);
    }

    @Override public boolean performFinish() {
        final String groupId = page.inputGroupId.getText();
        final String id = page.inputId.getText();
        final String version = page.inputVersion.getText();
        final String name = page.inputName.getText();

        final IRunnableWithProgress runnable = new IRunnableWithProgress() {
            public void run(IProgressMonitor monitor) throws InvocationTargetException {
                try {
                    doUpgrade(monitor, groupId, id, version, name);
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

    private void doUpgrade(IProgressMonitor monitor, final String groupId, final String id, final String version,
        final String name) throws Exception {
        final IWorkspaceRunnable upgradeRunnable = new IWorkspaceRunnable() {
            @Override public void run(IProgressMonitor workspaceMonitor) throws CoreException {
                try {
                    workspaceMonitor.beginTask("Upgrading language project", 3);
                    deleteUnused(id, name);
                    workspaceMonitor.worked(1);
                    upgradeProject();
                    workspaceMonitor.worked(1);
                    generateFiles(groupId, id, version, name);
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

    private void deleteUnused(String id, String name) throws Exception {
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
        NatureUtils.removeFrom("org.metaborg.spoofax.eclipse.meta.builder", eclipseProject);
        NatureUtils.removeFrom("org.eclipse.pde.PluginNature", eclipseProject);

        SpoofaxMetaNature.add(eclipseProject);
    }

    private void generateFiles(String groupId, String id, String version, String name) throws Exception {
        final ProjectSettings settings = new ProjectSettings(groupId, id, version, name, project);
        final NewProjectGenerator newProjectGenerator =
            new NewProjectGenerator(resourceService, settings, new String[] { "dummy" });
        newProjectGenerator.generateIgnoreFile();
        newProjectGenerator.generatePOM();
        final ProjectGenerator projectGenerator = new ProjectGenerator(resourceService, settings);
        projectGenerator.generateAll();
    }
}
