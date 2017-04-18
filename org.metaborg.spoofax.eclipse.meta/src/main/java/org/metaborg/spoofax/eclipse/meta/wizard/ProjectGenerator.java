package org.metaborg.spoofax.eclipse.meta.wizard;

import java.io.IOException;
import java.util.Collection;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.SubMonitor;
import org.metaborg.core.config.ConfigException;
import org.metaborg.core.language.LanguageIdentifier;
import org.metaborg.core.project.IProjectService;
import org.metaborg.core.project.ProjectException;
import org.metaborg.spoofax.eclipse.meta.nature.SpoofaxMetaNature;
import org.metaborg.spoofax.eclipse.nature.SpoofaxNature;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.eclipse.util.CommonNatures;
import org.metaborg.spoofax.eclipse.util.Nullable;
import org.metaborg.spoofax.meta.core.config.ISpoofaxLanguageSpecConfig;
import org.metaborg.spoofax.meta.core.config.ISpoofaxLanguageSpecConfigBuilder;
import org.metaborg.spoofax.meta.core.config.SdfVersion;
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

import com.google.inject.Inject;

public class ProjectGenerator {
    private final IEclipseResourceService resourceService;
    private final IProjectService projectService;
    private final ISpoofaxLanguageSpecService languageSpecService;
    private final ISpoofaxLanguageSpecConfigBuilder configBuilder;

    private final IWorkspace workspace;


    @Inject public ProjectGenerator(IEclipseResourceService resourceService, IProjectService projectService,
        ISpoofaxLanguageSpecService languageSpecService, ISpoofaxLanguageSpecConfigBuilder configBuilder) {
        this.resourceService = resourceService;
        this.projectService = projectService;
        this.languageSpecService = languageSpecService;
        this.configBuilder = configBuilder;

        this.workspace = ResourcesPlugin.getWorkspace();
    }


    public ISpoofaxLanguageSpec createLangSpecProject(LanguageIdentifier languageId, String languageName,
        Collection<String> extensions, SyntaxType syntaxType, AnalysisType analysisType, @Nullable IPath basePath,
        SubMonitor monitor) throws ProjectException, IOException, CoreException, ConfigException {
        monitor.setWorkRemaining(20);

        monitor.subTask("Generating language specification project");
        final String id = languageId.id;
        final IProject project = getProject(id);
        if(project.exists()) {
            throw new ProjectException(
                "Project " + project + " already exists, cannot create language specification project");
        }
        createProject(project, basePath);
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

        final LangSpecGeneratorSettings settings = settingsBuilder.build(location, configBuilder);
        final LangSpecGenerator newGenerator = new LangSpecGenerator(settings);
        newGenerator.generateAll();
        final @Nullable SdfVersion version;
        final boolean enabled;
        if(syntaxType == SyntaxType.SDF2) {
            version = SdfVersion.sdf2;
            enabled = true;
        } else if(syntaxType == SyntaxType.SDF3) {
            version = SdfVersion.sdf3;
            enabled = true;
        } else {
            version = null;
            enabled = false;
        }
        final ContinuousLanguageSpecGenerator generator =
            new ContinuousLanguageSpecGenerator(settings.generatorSettings, enabled, version);
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

    public void createExampleProject(ISpoofaxLanguageSpecConfig config, @Nullable String projectId,
        @Nullable IPath basePath, AnalysisType analysisType, SubMonitor monitor)
        throws IOException, ProjectException, CoreException {
        monitor.setWorkRemaining(3);

        monitor.subTask("Generating example project");
        final String baseId = config.identifier().id;
        final String id;
        if(projectId != null) {
            id = projectId;
        } else {
            id = LangProjectGenerator.siblingName(baseId);
        }
        final IProject project = getProject(id);
        if(project.exists()) {
            throw new ProjectException("Project " + project + " already exists, cannot create example project");
        }
        createProject(project, basePath);
        final FileObject location = resourceService.resolve(project);
        final GeneratorSettings settings = new GeneratorSettings(location, config, analysisType);
        final LangProjectGenerator generator = new LangProjectGenerator(settings);
        generator.generateAll();
        monitor.worked(1);

        monitor.subTask("Adding natures to example project");
        SpoofaxNature.add(project, monitor.newChild(1, SubMonitor.SUPPRESS_ALL_LABELS));
        CommonNatures.addMavenNature(project, monitor.newChild(1, SubMonitor.SUPPRESS_ALL_LABELS));

        monitor.done();
    }

    public void createTestProject(ISpoofaxLanguageSpecConfig config, @Nullable String projectId,
        @Nullable IPath basePath, SubMonitor monitor) throws IOException, ProjectException, CoreException {
        monitor.setWorkRemaining(3);

        monitor.subTask("Generating test project");
        final String baseId = config.identifier().id;
        final String id;
        if(projectId != null) {
            id = projectId;
        } else {
            id = LangTestGenerator.siblingName(baseId);
        }
        final IProject project = getProject(id);
        if(project.exists()) {
            throw new ProjectException("Project " + project + " already exists, cannot create test project");
        }
        createProject(project, basePath);
        final FileObject location = resourceService.resolve(project);
        final GeneratorSettings settings = new GeneratorSettings(location, config);
        final LangTestGenerator generator = new LangTestGenerator(settings);
        generator.generateAll();
        monitor.worked(1);

        monitor.subTask("Adding natures to test project");
        SpoofaxNature.add(project, monitor.newChild(1, SubMonitor.SUPPRESS_ALL_LABELS));
        CommonNatures.addMavenNature(project, monitor.newChild(1, SubMonitor.SUPPRESS_ALL_LABELS));

        monitor.done();
    }

    public void createEclipsePluginProject(ISpoofaxLanguageSpecConfig config, @Nullable String projectId,
        @Nullable IPath basePath, SubMonitor monitor) throws IOException, ProjectException, CoreException {
        monitor.setWorkRemaining(3);

        monitor.subTask("Generating Eclipse plugin project");
        final String baseId = config.identifier().id;
        final String id;
        if(projectId != null) {
            id = projectId;
        } else {
            id = EclipsePluginGenerator.siblingName(baseId);
        }
        final IProject project = getProject(id);
        if(project.exists()) {
            throw new ProjectException("Project " + project + " already exists, cannot create Eclipse plugin project");
        }
        createProject(project, basePath);
        final FileObject location = resourceService.resolve(project);
        final GeneratorSettings settings = new GeneratorSettings(location, config);
        final EclipsePluginGenerator generator = new EclipsePluginGenerator(settings);
        generator.generateAll();
        monitor.worked(1);

        monitor.subTask("Adding natures to Eclipse plugin project");
        CommonNatures.addMavenNature(project, monitor.newChild(1, SubMonitor.SUPPRESS_ALL_LABELS));
        CommonNatures.addPdePluginNature(project, monitor.newChild(1, SubMonitor.SUPPRESS_ALL_LABELS));

        monitor.done();
    }

    public void createEclipseFeatureProject(ISpoofaxLanguageSpecConfig config, @Nullable String projectId,
        @Nullable IPath basePath, SubMonitor monitor) throws IOException, ProjectException, CoreException {
        monitor.setWorkRemaining(3);

        monitor.subTask("Generating Eclipse feature project");
        final String baseId = config.identifier().id;
        final String id;
        if(projectId != null) {
            id = projectId;
        } else {
            id = EclipseFeatureGenerator.siblingName(baseId);
        }
        final IProject project = getProject(id);
        if(project.exists()) {
            throw new ProjectException("Project " + project + " already exists, cannot create Eclipse feature project");
        }
        createProject(project, basePath);
        final FileObject location = resourceService.resolve(project);
        final GeneratorSettings settings = new GeneratorSettings(location, config);
        final EclipseFeatureGenerator generator = new EclipseFeatureGenerator(settings);
        generator.generateAll();
        monitor.worked(1);

        monitor.subTask("Adding natures to Eclipse feature project");
        CommonNatures.addMavenNature(project, monitor.newChild(1, SubMonitor.SUPPRESS_ALL_LABELS));
        CommonNatures.addPdeFeatureNature(project, monitor.newChild(1, SubMonitor.SUPPRESS_ALL_LABELS));

        monitor.done();
    }

    public void createEclipseSiteProject(ISpoofaxLanguageSpecConfig config, @Nullable String projectId,
        @Nullable IPath basePath, SubMonitor monitor) throws IOException, ProjectException, CoreException {
        monitor.setWorkRemaining(3);

        monitor.subTask("Generating Eclipse update site project");
        final String baseId = config.identifier().id;
        final String id;
        if(projectId != null) {
            id = projectId;
        } else {
            id = EclipseSiteGenerator.siblingName(baseId);
        }
        final IProject project = getProject(id);
        if(project.exists()) {
            throw new ProjectException(
                "Project " + project + " already exists, cannot create Eclipse update site project");
        }
        createProject(project, basePath);
        final FileObject location = resourceService.resolve(project);
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

    private void createProject(IProject project, @Nullable IPath projectPath) throws CoreException {
        if(projectPath != null) {
            final String projectId = project.getName();
            final IProjectDescription description = workspace.newProjectDescription(projectId);
            projectPath = projectPath.append(projectId);
            description.setLocation(projectPath);
            project.create(description, null);
        } else {
            project.create(null);
        }
        project.open(null);
        project.refreshLocal(IResource.DEPTH_INFINITE, null);
    }
}
