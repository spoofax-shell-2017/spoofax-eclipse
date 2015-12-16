package org.metaborg.spoofax.eclipse.meta.project;

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
import org.metaborg.core.language.LanguageIdentifier;
import org.metaborg.core.language.LanguageVersion;
import org.metaborg.core.project.IProjectService;
import org.metaborg.core.project.settings.IProjectSettings;
import org.metaborg.core.project.settings.IProjectSettingsService;
import org.metaborg.core.project.settings.ProjectSettings;
import org.metaborg.spoofax.core.esv.ESVReader;
import org.metaborg.spoofax.core.project.settings.SpoofaxProjectSettings;
import org.metaborg.spoofax.core.terms.ITermFactoryService;
import org.metaborg.spoofax.eclipse.meta.nature.SpoofaxMetaNature;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.eclipse.util.BuilderUtils;
import org.metaborg.spoofax.eclipse.util.NatureUtils;
import org.metaborg.spoofax.eclipse.util.StatusUtils;
import org.metaborg.spoofax.generator.eclipse.language.EclipseProjectGenerator;
import org.metaborg.spoofax.generator.language.AnalysisType;
import org.metaborg.spoofax.generator.language.NewProjectGenerator;
import org.metaborg.spoofax.generator.language.ProjectGenerator;
import org.metaborg.spoofax.generator.project.GeneratorProjectSettings;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.resource.ContainsFileSelector;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.ParseError;
import org.spoofax.terms.io.binary.TermReader;

public class UpgradeLanguageProjectWizard extends Wizard {
    private static final ILogger logger = LoggerUtils.logger(UpgradeLanguageProjectWizard.class);

    private final IProject eclipseProject;
    private final FileObject projectLocation;
    private final UpgradeLanguageProjectWizardPage page;


    public UpgradeLanguageProjectWizard(IEclipseResourceService resourceService, IProjectService projectService,
        IProjectSettingsService projectSettingsService, ITermFactoryService termFactoryService, IProject eclipseProject) {
        this.eclipseProject = eclipseProject;
        this.projectLocation = resourceService.resolve(eclipseProject);

        String groupId = "";
        String id = "";
        String version = "";
        String name = "";

        // Try to get identifiers from packed.esv file.
        try {
            final FileObject[] files = projectLocation.findFiles(new ContainsFileSelector("packed.esv"));
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

        // Try to get identifiers from project settings.
        final org.metaborg.core.project.IProject metaborgProject = projectService.get(projectLocation);
        if(metaborgProject != null) {
            final IProjectSettings settings = projectSettingsService.get(metaborgProject);
            if(settings != null) {
                final LanguageIdentifier identifier = settings.identifier();
                groupId = groupId == null ? identifier.groupId : groupId;
                id = id == null ? identifier.id : id;
                version = version == null ? identifier.version.toString() : version;
                name = name == null ? settings.name() : name;
            }
        }
        
        // Try to get identifiers from generated settings file.
        final IProjectSettings settings = projectSettingsService.get(projectLocation);
        if(settings != null) {
            final LanguageIdentifier identifier = settings.identifier();
            groupId = groupId == null ? identifier.groupId : groupId;
            id = id == null ? identifier.id : id;
            version = version == null ? identifier.version.toString() : version;
            name = name == null ? settings.name() : name;
        }

        groupId = groupId == null ? "" : groupId;
        id = id == null ? "" : id;
        version = version == null ? "" : version;
        name = name == null ? "" : name;

        this.page = new UpgradeLanguageProjectWizardPage(groupId, id, version, name);
        addPage(this.page);

        setNeedsProgressMonitor(true);
    }

    @Override public boolean performFinish() {
        final String groupId = page.groupId();
        final String id = page.id();
        final String version = page.version();
        final String name = page.name();

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
            logger.error("Upgrading project failed", t);
            MessageDialog.openError(getShell(), "Error: " + t.getClass().getName(), t.getMessage());
            return false;
        }
        return true;
    }

    private void doUpgrade(IProgressMonitor monitor, final String groupId, final String id, final String versionString,
        final String name) throws Exception {
        final IWorkspaceRunnable upgradeRunnable = new IWorkspaceRunnable() {
            @Override public void run(IProgressMonitor workspaceMonitor) throws CoreException {
                try {
                    final LanguageVersion version = LanguageVersion.parse(versionString);
                    final LanguageIdentifier identifier = new LanguageIdentifier(groupId, id, version);
                    final IProjectSettings settings = new ProjectSettings(identifier, name);
                    final SpoofaxProjectSettings spoofaxSettings =
                        new SpoofaxProjectSettings(settings, projectLocation);
                    final GeneratorProjectSettings generatorSettings = new GeneratorProjectSettings(spoofaxSettings);

                    workspaceMonitor.beginTask("Upgrading language project", 4);
                    deleteUnused(id, name);
                    workspaceMonitor.worked(1);
                    upgradeProject(workspaceMonitor);
                    workspaceMonitor.worked(1);
                    upgradeClasspath(generatorSettings);
                    workspaceMonitor.worked(1);
                    generateFiles(generatorSettings);
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
        final FileObject impClassesDir = projectLocation.resolveFile("editor/java/" + impClassesLoc);
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
        final FileObject antBuilderDir = projectLocation.resolveFile(".externalToolBuilders");
        if(antBuilderDir.exists()) {
            antBuilderDir.resolveFile(name + " build.main.xml.launch").delete();
            antBuilderDir.resolveFile(name + " clean-project.xml.launch").delete();
            antBuilderDir.refresh();
            if(antBuilderDir.getChildren().length == 0) {
                antBuilderDir.delete();
            }
        }
        projectLocation.resolveFile("build.main.xml").delete();
        projectLocation.resolveFile("build.generated.xml").delete();

        // Delete Eclipse files
        projectLocation.resolveFile(".settings").delete(new AllFileSelector());
        projectLocation.resolveFile("META-INF").delete(new AllFileSelector());
        projectLocation.resolveFile("build.properties").delete();
        projectLocation.resolveFile("plugin.xml").delete();

        // Delete other files
        projectLocation.resolveFile(".cache").delete(new AllFileSelector());
        final FileObject libDir = projectLocation.resolveFile("lib");
        if(libDir.exists()) {
            libDir.resolveFile("runtime").delete(new AllFileSelector());
            libDir.resolveFile("editor-common.generated.str").delete();
            libDir.resolveFile("refactor-common.generated.str").delete();
            libDir.refresh();
            if(libDir.getChildren().length == 0) {
                libDir.delete();
            }
        }
        projectLocation.resolveFile("src-gen").delete(new AllFileSelector());
        projectLocation.resolveFile("target").delete(new AllFileSelector());
        projectLocation.resolveFile("utils").delete(new AllFileSelector());
        projectLocation.resolveFile(".gitignore").delete();
        projectLocation.resolveFile("pom.xml").delete();
    }

    private void upgradeProject(IProgressMonitor monitor) throws Exception {
        // Remove legacy builders and natures
        BuilderUtils.removeFrom("org.eclipse.ui.externaltools.ExternalToolBuilder", eclipseProject, monitor);
        BuilderUtils.removeFrom("org.eclipse.pde.ManifestBuilder", eclipseProject, monitor);
        BuilderUtils.removeFrom("org.eclipse.pde.SchemaBuilder", eclipseProject, monitor);
        NatureUtils.removeFrom("org.strategoxt.imp.metatooling.nature", eclipseProject, monitor);
        NatureUtils.removeFrom("org.metaborg.spoofax.eclipse.meta.builder", eclipseProject, monitor);
        NatureUtils.removeFrom("org.eclipse.pde.PluginNature", eclipseProject, monitor);

        SpoofaxMetaNature.add(eclipseProject, monitor);
    }

    private void upgradeClasspath(GeneratorProjectSettings settings) throws Exception {
        final FileObject classpath = projectLocation.resolveFile(".classpath");
        classpath.delete();
        final EclipseProjectGenerator generator = new EclipseProjectGenerator(settings);
        generator.generateClasspath();
    }

    private void generateFiles(GeneratorProjectSettings settings) throws Exception {
        final NewProjectGenerator newGenerator = new NewProjectGenerator(settings, AnalysisType.NaBL_TS);
        newGenerator.generateIgnoreFile();
        newGenerator.generatePOM();
        final ProjectGenerator generator = new ProjectGenerator(settings);
        generator.generateAll();
    }
}
