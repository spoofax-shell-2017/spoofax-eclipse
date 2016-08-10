package org.metaborg.spoofax.eclipse.meta.wizard;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Collection;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
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
import org.metaborg.core.project.IProjectService;
import org.metaborg.core.project.ProjectException;
import org.metaborg.spoofax.eclipse.meta.SpoofaxMetaPlugin;
import org.metaborg.spoofax.eclipse.meta.nature.SpoofaxMetaNature;
import org.metaborg.spoofax.eclipse.nature.SpoofaxNature;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.eclipse.util.CommonNatures;
import org.metaborg.spoofax.eclipse.util.Nullable;
import org.metaborg.spoofax.meta.core.config.ISpoofaxLanguageSpecConfig;
import org.metaborg.spoofax.meta.core.config.ISpoofaxLanguageSpecConfigBuilder;
import org.metaborg.spoofax.meta.core.generator.GeneratorSettings;
import org.metaborg.spoofax.meta.core.generator.eclipse.EclipseFeatureGenerator;
import org.metaborg.spoofax.meta.core.generator.eclipse.EclipseLangSpecGenerator;
import org.metaborg.spoofax.meta.core.generator.eclipse.EclipsePluginGenerator;
import org.metaborg.spoofax.meta.core.generator.eclipse.EclipseSiteGenerator;
import org.metaborg.spoofax.meta.core.generator.general.AnalysisType;
import org.metaborg.spoofax.meta.core.generator.general.ContinuousLanguageSpecGenerator;
import org.metaborg.spoofax.meta.core.generator.general.LangProjectGenerator;
import org.metaborg.spoofax.meta.core.generator.general.LangSpecGenerator;
import org.metaborg.spoofax.meta.core.generator.general.LangSpecGeneratorSettings;
import org.metaborg.spoofax.meta.core.generator.general.LangSpecGeneratorSettingsBuilder;
import org.metaborg.spoofax.meta.core.generator.general.LangTestGenerator;
import org.metaborg.spoofax.meta.core.generator.general.SyntaxType;
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpec;
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpecService;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.inject.Injector;

public class CreateLangSpecWizard extends Wizard implements INewWizard {
    private static final ILogger logger = LoggerUtils.logger(CreateLangSpecWizard.class);

    private final IEclipseResourceService resourceService;
    private final IProjectService projectService;
    private final ISpoofaxLanguageSpecService languageSpecService;
    private final ISpoofaxLanguageSpecConfigBuilder configBuilder;

    private final IWorkspace workspace;
    private final CreateLangSpecWizardPage page;


    public CreateLangSpecWizard() {
        final Injector injector = SpoofaxMetaPlugin.injector();

        this.resourceService = injector.getInstance(IEclipseResourceService.class);
        this.projectService = injector.getInstance(IProjectService.class);
        this.languageSpecService = injector.getInstance(ISpoofaxLanguageSpecService.class);
        this.configBuilder = injector.getInstance(ISpoofaxLanguageSpecConfigBuilder.class);

        this.workspace = ResourcesPlugin.getWorkspace();
        this.page = new CreateLangSpecWizardPage();

        addPage(this.page);
        setNeedsProgressMonitor(true);
    }


    @Override public void init(IWorkbench workbench, IStructuredSelection selection) {
    }


    @Override public boolean performFinish() {
        final IProject project = page.getProjectHandle();
        final URI projectLocation;
        if(page.useDefaults()) {
            projectLocation = null;
        } else {
            projectLocation = page.getLocationURI();
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
                        generateEclipseFeatureProject, generateEclipseUpdatesiteProject, project, projectLocation);
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
        boolean generateEclipseUpdatesiteProject, IProject langSpecProject, @Nullable URI langSpecProjectLocation)
        throws ProjectException, IOException, CoreException, ConfigException, OperationCanceledException {
        final SubMonitor monitor = SubMonitor.convert(rootMonitor, "Generating language projects",
            100 + (generateExampleProject ? 1 : 0) + (generateTestProject ? 1 : 0)
                + (generateEclipsePluginProject ? 1 : 0) + (generateEclipseFeatureProject ? 1 : 0)
                + (generateEclipseUpdatesiteProject ? 1 : 0));

        createProject(langSpecProject, langSpecProjectLocation);
        final ISpoofaxLanguageSpec langSpec = createLangSpecProject(monitor.newChild(100), languageId, languageName,
            extensions, syntaxType, analysisType, langSpecProject);

        final ISpoofaxLanguageSpecConfig config = langSpec.config();
        final @Nullable URI baseDirURI;
        final FileObject baseDir;
        if(langSpecProjectLocation != null) {
            final URI uri = langSpecProjectLocation;
            baseDirURI = uri.getPath().endsWith("/") ? uri.resolve("..") : uri.resolve(".");
            baseDir = resourceService.resolve(baseDirURI);
        } else {
            baseDirURI = null;
            baseDir = resourceService.resolveWorkspaceRoot();
        }

        if(generateExampleProject) {
            createExampleProject(monitor.newChild(1), config, baseDirURI, baseDir);
        }
        if(generateTestProject) {
            createTestProject(monitor.newChild(1), config, baseDirURI, baseDir);
        }
        if(generateEclipsePluginProject) {
            createEclipsePluginProject(monitor.newChild(1), config, baseDirURI, baseDir);
        }
        if(generateEclipseFeatureProject) {
            createEclipseFeatureProject(monitor.newChild(1), config, baseDirURI, baseDir);
        }
        if(generateEclipseUpdatesiteProject) {
            createEclipseSiteProject(monitor.newChild(1), config, baseDirURI, baseDir);
        }

        monitor.done();
    }

    private ISpoofaxLanguageSpec createLangSpecProject(SubMonitor monitor, LanguageIdentifier languageId,
        String languageName, Collection<String> extensions, SyntaxType syntaxType, AnalysisType analysisType,
        IProject project) throws ProjectException, IOException, CoreException, ConfigException {
        monitor.setWorkRemaining(20);

        final FileObject location = resourceService.resolve(project);

        // @formatter:off
        final LangSpecGeneratorSettingsBuilder settingsBuilder = new LangSpecGeneratorSettingsBuilder()
            .withGroupId(languageId.groupId)
            .withId(languageId.id)
            .withVersion(languageId.version)
            .withName(languageName)
            .withExtensions(extensions)
            .withSyntaxType(syntaxType)
            .withAnalysisType(analysisType)
            ;
        // @formatter:on

        monitor.subTask("Generating language specification project");
        final LangSpecGeneratorSettings settings = settingsBuilder.build(location, configBuilder);
        final LangSpecGenerator newGenerator = new LangSpecGenerator(settings);
        newGenerator.generateAll();
        final ContinuousLanguageSpecGenerator generator =
            new ContinuousLanguageSpecGenerator(settings.generatorSettings);
        generator.generateAll();
        final EclipseLangSpecGenerator eclipseGenerator = new EclipseLangSpecGenerator(settings.generatorSettings);
        eclipseGenerator.generateAll();
        monitor.worked(2);

        monitor.subTask("Refreshing language specification project");
        project.refreshLocal(IResource.DEPTH_INFINITE, monitor.newChild(2, SubMonitor.SUPPRESS_ALL_LABELS));

        monitor.subTask("Adding natures to language specification project");
        SpoofaxMetaNature.add(project, monitor.newChild(1, SubMonitor.SUPPRESS_ALL_LABELS));

        monitor.subTask("Building language specification project");
        project.build(IncrementalProjectBuilder.FULL_BUILD, monitor.newChild(15, SubMonitor.SUPPRESS_ALL_LABELS));

        final org.metaborg.core.project.IProject metaborgProject = projectService.get(location);
        final ISpoofaxLanguageSpec langSpec = languageSpecService.get(metaborgProject);

        monitor.done();
        return langSpec;
    }

    private void createExampleProject(SubMonitor monitor, ISpoofaxLanguageSpecConfig config, @Nullable URI baseDirURI,
        FileObject baseDir) throws IOException, ProjectException, CoreException {
        monitor.setWorkRemaining(3);

        monitor.subTask("Generating example project");
        final String id = config.identifier().id;
        final IProject project = getProject(LangProjectGenerator.siblingName(id));
        if(project.exists()) {
            throw new ProjectException("Project " + project + " already exists, cannot create example project");
        }
        createProject(project, baseDirURI);
        final FileObject location = LangProjectGenerator.siblingDir(baseDir, id);
        final GeneratorSettings settings = new GeneratorSettings(location, config);
        final LangProjectGenerator generator = new LangProjectGenerator(settings);
        generator.generateAll();
        monitor.worked(1);

        monitor.subTask("Adding natures to example project");
        SpoofaxNature.add(project, monitor.newChild(1, SubMonitor.SUPPRESS_ALL_LABELS));
        CommonNatures.addMavenNature(project, monitor.newChild(1, SubMonitor.SUPPRESS_ALL_LABELS));

        monitor.done();
    }

    private void createTestProject(SubMonitor monitor, ISpoofaxLanguageSpecConfig config, @Nullable URI baseDirURI,
        FileObject baseDir) throws IOException, ProjectException, CoreException {
        monitor.setWorkRemaining(3);

        monitor.subTask("Generating test project");
        final String id = config.identifier().id;
        final IProject project = getProject(LangTestGenerator.siblingName(id));
        if(project.exists()) {
            throw new ProjectException("Project " + project + " already exists, cannot create test project");
        }
        createProject(project, baseDirURI);
        final FileObject location = LangTestGenerator.siblingDir(baseDir, id);
        final GeneratorSettings settings = new GeneratorSettings(location, config);
        final LangTestGenerator generator = new LangTestGenerator(settings);
        generator.generateAll();
        monitor.worked(1);

        monitor.subTask("Adding natures to test project");
        SpoofaxNature.add(project, monitor.newChild(1, SubMonitor.SUPPRESS_ALL_LABELS));
        CommonNatures.addMavenNature(project, monitor.newChild(1, SubMonitor.SUPPRESS_ALL_LABELS));

        monitor.done();
    }

    private void createEclipsePluginProject(SubMonitor monitor, ISpoofaxLanguageSpecConfig config,
        @Nullable URI baseDirURI, FileObject baseDir) throws IOException, ProjectException, CoreException {
        monitor.setWorkRemaining(3);

        monitor.subTask("Generating Eclipse plugin project");
        final String id = config.identifier().id;
        final IProject project = getProject(EclipsePluginGenerator.siblingName(id));
        if(project.exists()) {
            throw new ProjectException("Project " + project + " already exists, cannot create Eclipse plugin project");
        }
        createProject(project, baseDirURI);
        final FileObject location = EclipsePluginGenerator.siblingDir(baseDir, id);
        final GeneratorSettings settings = new GeneratorSettings(location, config);
        final EclipsePluginGenerator generator = new EclipsePluginGenerator(settings);
        generator.generateAll();
        monitor.worked(1);

        monitor.subTask("Adding natures to Eclipse plugin project");
        CommonNatures.addMavenNature(project, monitor.newChild(1, SubMonitor.SUPPRESS_ALL_LABELS));
        CommonNatures.addPdePluginNature(project, monitor.newChild(1, SubMonitor.SUPPRESS_ALL_LABELS));

        monitor.done();
    }

    private void createEclipseFeatureProject(SubMonitor monitor, ISpoofaxLanguageSpecConfig config,
        @Nullable URI baseDirURI, FileObject baseDir) throws IOException, ProjectException, CoreException {
        monitor.setWorkRemaining(3);

        monitor.subTask("Generating Eclipse feature project");
        final String id = config.identifier().id;
        final IProject project = getProject(EclipseFeatureGenerator.siblingName(id));
        if(project.exists()) {
            throw new ProjectException("Project " + project + " already exists, cannot create Eclipse feature project");
        }
        createProject(project, baseDirURI);
        final FileObject location = EclipseFeatureGenerator.siblingDir(baseDir, id);
        final GeneratorSettings settings = new GeneratorSettings(location, config);
        final EclipseFeatureGenerator generator = new EclipseFeatureGenerator(settings);
        generator.generateAll();
        monitor.worked(1);

        monitor.subTask("Adding natures to Eclipse feature project");
        CommonNatures.addMavenNature(project, monitor.newChild(1, SubMonitor.SUPPRESS_ALL_LABELS));
        CommonNatures.addPdeFeatureNature(project, monitor.newChild(1, SubMonitor.SUPPRESS_ALL_LABELS));

        monitor.done();
    }

    private void createEclipseSiteProject(SubMonitor monitor, ISpoofaxLanguageSpecConfig config,
        @Nullable URI baseDirURI, FileObject baseDir) throws IOException, ProjectException, CoreException {
        monitor.setWorkRemaining(3);

        monitor.subTask("Generating Eclipse update site project");
        final String id = config.identifier().id;
        final IProject project = getProject(EclipseSiteGenerator.siblingName(id));
        if(project.exists()) {
            throw new ProjectException(
                "Project " + project + " already exists, cannot create Eclipse update site project");
        }
        createProject(project, baseDirURI);
        final FileObject location = EclipseSiteGenerator.siblingDir(baseDir, id);
        final GeneratorSettings settings = new GeneratorSettings(location, config);
        final EclipseSiteGenerator generator = new EclipseSiteGenerator(settings);
        generator.generateAll();
        monitor.worked(1);

        monitor.subTask("Adding natures to Eclipse update site project");
        CommonNatures.addMavenNature(project, monitor.newChild(1, SubMonitor.SUPPRESS_ALL_LABELS));
        CommonNatures.addPdeSiteNature(project, monitor.newChild(1, SubMonitor.SUPPRESS_ALL_LABELS));

        monitor.done();
    }


    private IProject getProject(String name) {
        return workspace.getRoot().getProject(name);
    }

    private void createProject(IProject project, @Nullable URI projectLocation) throws CoreException {
        if(projectLocation != null) {
            final IProjectDescription description = workspace.newProjectDescription(project.getName());
            description.setLocationURI(projectLocation);
            project.create(description, null);
        } else {
            project.create(null);
        }
        project.open(null);
        project.refreshLocal(IResource.DEPTH_INFINITE, null);
    }
}
