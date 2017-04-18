package org.metaborg.spoofax.eclipse.meta.wizard;

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
import org.metaborg.core.config.ConfigException;
import org.metaborg.core.language.LanguageIdentifier;
import org.metaborg.core.language.LanguageVersion;
import org.metaborg.core.project.IProjectService;
import org.metaborg.meta.core.config.ILanguageSpecConfig;
import org.metaborg.meta.core.project.ILanguageSpec;
import org.metaborg.meta.core.project.ILanguageSpecService;
import org.metaborg.spoofax.core.esv.ESVReader;
import org.metaborg.spoofax.core.terms.ITermFactoryService;
import org.metaborg.spoofax.eclipse.meta.nature.SpoofaxMetaNature;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.eclipse.util.BuilderUtils;
import org.metaborg.spoofax.eclipse.util.NatureUtils;
import org.metaborg.spoofax.eclipse.util.Nullable;
import org.metaborg.spoofax.eclipse.util.StatusUtils;
import org.metaborg.spoofax.meta.core.config.ISpoofaxLanguageSpecConfigBuilder;
import org.metaborg.spoofax.meta.core.config.SdfVersion;
import org.metaborg.spoofax.meta.core.generator.GeneratorSettings;
import org.metaborg.spoofax.meta.core.generator.eclipse.EclipseLangSpecGenerator;
import org.metaborg.spoofax.meta.core.generator.general.ContinuousLanguageSpecGenerator;
import org.metaborg.spoofax.meta.core.generator.general.LangSpecGenerator;
import org.metaborg.spoofax.meta.core.generator.general.LangSpecGeneratorSettings;
import org.metaborg.spoofax.meta.core.generator.general.LangSpecGeneratorSettingsBuilder;
import org.metaborg.spoofax.meta.core.generator.general.SyntaxType;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.resource.ContainsFileSelector;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.ParseError;
import org.spoofax.terms.io.binary.TermReader;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

public class UpgradeLangSpecWizard extends Wizard {
    private static final ILogger logger = LoggerUtils.logger(UpgradeLangSpecWizard.class);

    private final IProject eclipseProject;
    private final FileObject projectLocation;
    private final UpgradeLangSpecWizardPage page;

    private final ISpoofaxLanguageSpecConfigBuilder configBuilder;


    public UpgradeLangSpecWizard(IEclipseResourceService resourceService, IProjectService projectService,
        ILanguageSpecService languageSpecService, ISpoofaxLanguageSpecConfigBuilder configBuilder,
        ITermFactoryService termFactoryService, IProject eclipseProject) {
        this.configBuilder = configBuilder;
        this.eclipseProject = eclipseProject;
        this.projectLocation = resourceService.resolve(eclipseProject);

        String groupId = "";
        String id = "";
        String version = "";
        String name = "";

        // Try to get identifiers from old packed.esv file.
        try {
            final FileObject[] files = projectLocation.findFiles(new ContainsFileSelector("packed.esv"));
            if(files != null && files.length > 0) {
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
            // Ignore
        }

        // Try to get identifiers from language specification configuration.
        try {
            final org.metaborg.core.project.IProject metaborgProject = projectService.get(projectLocation);
            final ILanguageSpec languageSpec = languageSpecService.get(metaborgProject);
            if(languageSpec != null) {
                final ILanguageSpecConfig config = languageSpec.config();
                if(config != null) {
                    final LanguageIdentifier identifier = config.identifier();
                    groupId = Strings.isNullOrEmpty(groupId) ? identifier.groupId : groupId;
                    id = Strings.isNullOrEmpty(id) ? identifier.id : id;
                    version = Strings.isNullOrEmpty(version) ? identifier.version.toString() : version;
                    name = Strings.isNullOrEmpty(name) ? config.name() : name;
                }
            }
        } catch(ConfigException e) {
            // Ignore
        }

        this.page = new UpgradeLangSpecWizardPage(groupId, id, version, name);

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

                    // @formatter:off
                    final LangSpecGeneratorSettingsBuilder settingsBuilder = new LangSpecGeneratorSettingsBuilder()
                        .withGroupId(groupId)
                        .withId(id)
                        .withVersion(version)
                        .withName(name)
                        .withExtensions(Lists.<String>newArrayList())
                        //.withSyntaxType(syntaxType)
                        //.withAnalysisType(analysisType)
                        ;
                    // @formatter:on

                    final LangSpecGeneratorSettings settings = settingsBuilder.build(projectLocation, configBuilder);

                    workspaceMonitor.beginTask("Upgrading language project", 4);
                    deleteUnused(id, name);
                    workspaceMonitor.worked(1);
                    upgradeProject(workspaceMonitor);
                    workspaceMonitor.worked(1);
                    upgradeClasspath(settings.generatorSettings);
                    workspaceMonitor.worked(1);
                    generateFiles(settings);
                    workspaceMonitor.worked(1);
                } catch(CoreException e) {
                    throw e;
                } catch(Exception e) {
                    throw new CoreException(StatusUtils.error("Upgrading language project failed unexpectedly", e));
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

    private void upgradeClasspath(GeneratorSettings settings) throws Exception {
        final FileObject classpath = projectLocation.resolveFile(".classpath");
        classpath.delete();
        final EclipseLangSpecGenerator generator = new EclipseLangSpecGenerator(settings);
        generator.generateClasspath();
    }

    private void generateFiles(LangSpecGeneratorSettings settings) throws Exception {
        final LangSpecGenerator newGenerator = new LangSpecGenerator(settings);
        newGenerator.generateIgnoreFile();
        newGenerator.generatePOM();
        final @Nullable SdfVersion version;
        final boolean enabled;
        if(settings.syntaxType == SyntaxType.SDF2) {
            version = SdfVersion.sdf2;
            enabled = true;
        } else if(settings.syntaxType == SyntaxType.SDF3) {
            version = SdfVersion.sdf3;
            enabled = true;
        } else {
            version = null;
            enabled = false;
        }
        final ContinuousLanguageSpecGenerator generator =
            new ContinuousLanguageSpecGenerator(settings.generatorSettings, enabled, version);
        generator.generateAll();
    }
}
