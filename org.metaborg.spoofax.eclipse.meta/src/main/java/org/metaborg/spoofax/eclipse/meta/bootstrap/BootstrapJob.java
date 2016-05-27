package org.metaborg.spoofax.eclipse.meta.bootstrap;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.vfs2.AllFileSelector;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.config.ConfigException;
import org.metaborg.core.language.ILanguageComponent;
import org.metaborg.core.language.ILanguageDiscoveryRequest;
import org.metaborg.core.language.ILanguageDiscoveryService;
import org.metaborg.core.language.ILanguageService;
import org.metaborg.core.language.LanguageIdentifier;
import org.metaborg.core.language.LanguageVersion;
import org.metaborg.core.project.IProject;
import org.metaborg.core.project.IProjectService;
import org.metaborg.spoofax.eclipse.meta.SpoofaxMetaPlugin;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.eclipse.util.Nullable;
import org.metaborg.spoofax.eclipse.util.StatusUtils;
import org.metaborg.spoofax.meta.core.config.ISpoofaxLanguageSpecConfig;
import org.metaborg.spoofax.meta.core.config.ISpoofaxLanguageSpecConfigBuilder;
import org.metaborg.spoofax.meta.core.config.ISpoofaxLanguageSpecConfigWriter;
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpec;
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpecService;
import org.metaborg.util.collection.BiLinkedHashMultimap;
import org.metaborg.util.collection.BiSetMultimap;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;

public class BootstrapJob extends Job {
    private static final ILogger logger = LoggerUtils.logger(BootstrapJob.class);

    private final IEclipseResourceService resourceService;
    private final IProjectService projectService;
    private final ILanguageService languageService;
    private final ILanguageDiscoveryService languageDiscoveryService;

    private final Provider<ISpoofaxLanguageSpecConfigBuilder> languageSpecConfigBuilderProvider;
    private final ISpoofaxLanguageSpecConfigWriter languageSpecConfigWriter;
    private final ISpoofaxLanguageSpecService languageSpecService;

    private final IWorkspaceRoot workspaceRoot;
    private final org.eclipse.core.resources.IProject targetEclipseProject;


    @Inject public BootstrapJob(IEclipseResourceService resourceService, IProjectService projectService,
        ILanguageService languageService, ILanguageDiscoveryService languageDiscoveryService,
        Provider<ISpoofaxLanguageSpecConfigBuilder> languageSpecConfigBuilderProvider,
        ISpoofaxLanguageSpecConfigWriter languageSpecConfigWriter, ISpoofaxLanguageSpecService languageSpecService,
        @Assisted IWorkspaceRoot workspaceRoot, @Assisted org.eclipse.core.resources.IProject targetEclipseProject) {
        super("Bootstrapping " + targetEclipseProject);
        this.resourceService = resourceService;
        this.projectService = projectService;
        this.languageService = languageService;
        this.languageDiscoveryService = languageDiscoveryService;

        this.languageSpecConfigBuilderProvider = languageSpecConfigBuilderProvider;
        this.languageSpecConfigWriter = languageSpecConfigWriter;
        this.languageSpecService = languageSpecService;

        this.workspaceRoot = workspaceRoot;
        this.targetEclipseProject = targetEclipseProject;
    }


    @Override protected IStatus run(IProgressMonitor rootMonitor) {
        final SubMonitor monitor = SubMonitor.convert(rootMonitor);
        monitor.setWorkRemaining(7);


        // Gather all projects
        if(monitor.isCanceled()) {
            return StatusUtils.cancel();
        }
        monitor.setTaskName("Gathering language specification projects");
        final Map<String, BootstrapProject> projects = Maps.newHashMap();
        for(org.eclipse.core.resources.IProject eclipseProject : workspaceRoot.getProjects()) {
            if(monitor.isCanceled()) {
                return StatusUtils.cancel();
            }

            if(!eclipseProject.isOpen()) {
                continue;
            }

            final BootstrapProject bootstrapProject;
            try {
                bootstrapProject = toBootstrapProject(eclipseProject);
                if(bootstrapProject == null) {
                    // Project is not a language specification, skip.
                    continue;
                }
            } catch(ConfigException e) {
                return error("Cannot get language specification config for {}", e, eclipseProject);
            }
            final String id = bootstrapProject.idNoVersion();

            if(projects.containsKey(id)) {
                return error("Project {} already exists", bootstrapProject);
            }

            logger.info("Adding project {}", bootstrapProject);
            projects.put(id, bootstrapProject);
        }
        monitor.worked(1);


        // Check target project
        if(monitor.isCanceled()) {
            return StatusUtils.cancel();
        }
        monitor.setTaskName("Checking target project");
        final BootstrapProject targetProject;
        try {
            final BootstrapProject tempTargetProject = toBootstrapProject(targetEclipseProject);
            if(tempTargetProject == null) {
                return error("Target project {} is not a language specification project", targetEclipseProject);
            }
            if(!projects.containsValue(tempTargetProject)) {
                return error("Target project {} was not found in the workspace", tempTargetProject);
            }
            // Get target project from projects map, such that the instance is shared.
            // This is important because the language specification inside a bootstrap project can be updated later.
            targetProject = projects.get(tempTargetProject.idNoVersion());
        } catch(ConfigException e) {
            return error("Cannot get language specification config for target project {}", e, targetEclipseProject);
        }
        monitor.worked(1);


        // Set up dependency graph
        if(monitor.isCanceled()) {
            return StatusUtils.cancel();
        }
        monitor.setTaskName("Creating dependency graph");
        final BiSetMultimap<BootstrapProject, BootstrapProject> dependencies = new BiLinkedHashMultimap<>();
        for(BootstrapProject project : projects.values()) {
            if(monitor.isCanceled()) {
                return StatusUtils.cancel();
            }
            final ISpoofaxLanguageSpecConfig config = project.config();
            for(LanguageIdentifier dep : config.compileDeps()) {
                if(monitor.isCanceled()) {
                    return StatusUtils.cancel();
                }
                final String id = BootstrapProject.toNoVersionId(dep);
                if(!projects.containsKey(id)) {
                    logger.warn("Project with id {} does not exist, skipping", id);
                    continue;
                }
                final BootstrapProject depProject = projects.get(id);
                logger.info("Adding dependency from {} -> {}", project.idNoVersion(), id);
                dependencies.put(project, depProject);
            }
        }
        monitor.worked(1);


        // Set target project version.
        if(monitor.isCanceled()) {
            return StatusUtils.cancel();
        }
        monitor.setTaskName("Setting new version for target project");
        final LanguageVersion nextVersion = nextVersion(targetProject);
        logger.info("Next version is {}", nextVersion);
        try {
            setVersion(targetProject, nextVersion);
        } catch(ConfigException e) {
            return error("Unable to set version to {} for target project {}", e, nextVersion, targetProject);
        }
        monitor.worked(1);


        // Get previous binary
        // final ILanguageComponent previousComponent = languageService.getComponent(targetProject.identifier());
        // if(previousComponent == null) {
        // return error("Unable to get previous language component for target project {}", targetProject);
        // }
        // final FileObject


        // Clean, build, reload
        if(monitor.isCanceled()) {
            return StatusUtils.cancel();
        }
        monitor.setTaskName("Clean, build, and reload target project");
        try {
            final SubMonitor subMonitor = monitor.newChild(1);
            final FileObject binary = build(targetProject, subMonitor);
            final FileObject storedBinary = storeBinary(targetProject, binary);
            compare(binary, storedBinary);
            reloadLanguage(storedBinary);
            subMonitor.done();
        } catch(CoreException | IOException | MetaborgException e) {
            return error("Cleaning and building project {} failed ", e, targetProject);
        }


        // Set dependency versions.
        if(monitor.isCanceled()) {
            return StatusUtils.cancel();
        }
        monitor.setTaskName("Setting new version for dependencies");
        try {
            setDependencyVersions(projects.values(), targetProject.identifier().groupId, targetProject.identifier().id,
                nextVersion);
        } catch(ConfigException e) {
            return error("Unable to set version to {} for dependencies", e, nextVersion);
        }
        monitor.worked(1);


        // Build everything against new version.
        
        
        
        // Fixpoint
        
        

        monitor.done();
        return StatusUtils.success();
    }


    private LanguageVersion nextVersion(BootstrapProject project) {
        final LanguageVersion version = project.config().identifier().version;
        return new LanguageVersion(version.major, version.minor, version.patch + 1, "");
    }

    private void setVersion(BootstrapProject project, LanguageVersion newVersion) throws ConfigException {
        logger.info("Setting version of {} to {}", project, newVersion);

        final ISpoofaxLanguageSpecConfig config = project.config();
        final ISpoofaxLanguageSpecConfigBuilder languageSpecConfigBuilder = languageSpecConfigBuilderProvider.get();
        languageSpecConfigBuilder.copyFrom(config);

        // Set version
        final LanguageIdentifier identifier = config.identifier();
        final LanguageIdentifier newIdentifier = new LanguageIdentifier(identifier.groupId, identifier.id, newVersion);
        languageSpecConfigBuilder.withIdentifier(newIdentifier);

        // Update configuration
        updateConfig(project, languageSpecConfigBuilder);
    }

    private void setDependencyVersion(BootstrapProject project, String groupId, String id, LanguageVersion newVersion)
        throws ConfigException {
        logger.info("Setting dependency versions of {}:{} in {} to {}", groupId, id, project, newVersion);

        final ISpoofaxLanguageSpecConfig config = project.config();
        final ISpoofaxLanguageSpecConfigBuilder languageSpecConfigBuilder = languageSpecConfigBuilderProvider.get();
        languageSpecConfigBuilder.copyFrom(config);

        // Set versions in source dependencies.
        final Collection<LanguageIdentifier> newSourceDeps = config.sourceDeps();
        for(LanguageIdentifier depId : config.sourceDeps()) {
            if(depId.groupId.equals(groupId) && depId.id.equals(id)) {
                newSourceDeps.remove(depId);
                newSourceDeps.add(new LanguageIdentifier(depId.groupId, depId.id, newVersion));
                logger.info("Setting source dependency {}:{} version to {}", groupId, id, newVersion);
            }
        }
        languageSpecConfigBuilder.withSourceDeps(newSourceDeps);

        // Set versions in compile dependencies.
        final Collection<LanguageIdentifier> newCompileDeps = config.compileDeps();
        for(LanguageIdentifier depId : config.compileDeps()) {
            if(depId.groupId.equals(groupId) && depId.id.equals(id)) {
                newCompileDeps.remove(depId);
                newCompileDeps.add(new LanguageIdentifier(depId.groupId, depId.id, newVersion));
                logger.info("Setting compile dependency {}:{} version to {}", groupId, id, newVersion);
            }
        }
        languageSpecConfigBuilder.withCompileDeps(newCompileDeps);

        // Update configuration
        updateConfig(project, languageSpecConfigBuilder);
    }

    private void updateConfig(BootstrapProject project, ISpoofaxLanguageSpecConfigBuilder languageSpecConfigBuilder)
        throws ConfigException {
        // Write configuration file to disk.
        final ISpoofaxLanguageSpecConfig newConfig = languageSpecConfigBuilder.build(project.location());
        languageSpecConfigWriter.write(project.languageSpec(), newConfig, null);

        // Update bootstrap project with new language specification project.
        final ISpoofaxLanguageSpec newLanguageSpec = languageSpecService.get(project.languageSpec());
        project.updateLanguageSpec(newLanguageSpec);
    }

    private void setDependencyVersions(Iterable<BootstrapProject> projects, String groupId, String id,
        LanguageVersion newVersion) throws ConfigException {
        for(BootstrapProject project : projects) {
            setDependencyVersion(project, groupId, id, newVersion);
        }
    }


    private FileObject build(BootstrapProject project, SubMonitor monitor) throws CoreException {
        monitor.setWorkRemaining(2);
        logger.info("Cleaning {}", project);
        project.eclipseProject.build(IncrementalProjectBuilder.CLEAN_BUILD, monitor.newChild(1));
        logger.info("Building {}", project);
        project.eclipseProject.build(IncrementalProjectBuilder.FULL_BUILD, monitor.newChild(1));
        return project.paths().spxArchiveFile(project.config().identifier().toFileString());
    }

    private FileObject storeBinary(BootstrapProject project, FileObject binary) throws FileSystemException {
        final IPath statePath = SpoofaxMetaPlugin.plugin().getStateLocation();
        final FileObject stateDir = resourceService.resolve(statePath.toString());
        final LanguageIdentifier id = project.identifier();
        // @formatter:off
        final FileObject storeFile = stateDir
            .resolveFile("bootstrap")
            .resolveFile(id.groupId)
            .resolveFile(id.id)
            .resolveFile(id.version.toString())
            .resolveFile(binary.getName().getBaseName())
            ;
        // @formatter:on
        logger.info("Storing binary for {} at {}", project, storeFile);
        storeFile.createFile();
        storeFile.copyFrom(binary, new AllFileSelector());
        return storeFile;
    }

    private boolean compare(FileObject leftBinary, FileObject rightBinary) throws IOException {
        logger.info("Comparing {} to {}", leftBinary, rightBinary);
        final ResourceComparer comparer = new ResourceComparer(resourceService, null);
        final Collection<ResourceDiff> diffs = comparer.compare(leftBinary, rightBinary);
        if(diffs.isEmpty()) {
            logger.info("Binaries {} and {} are identical", leftBinary, rightBinary);
            return true;
        }
        for(ResourceDiff diff : diffs) {
            logger.warn(diff.toString());
        }
        return false;
    }

    private Iterable<ILanguageComponent> reloadLanguage(FileObject binary) throws MetaborgException {
        final FileObject zipBinary = resourceService.resolve("zip:" + binary.getName().getURI() + "!/");
        logger.info("Reloading language implementation at {}", binary);
        final Iterable<ILanguageDiscoveryRequest> requests = languageDiscoveryService.request(zipBinary);
        final Iterable<ILanguageComponent> components = languageDiscoveryService.discover(requests);
        return components;
    }



    private IStatus error(String fmt, Object... args) {
        final String message = logger.format(fmt, args);
        logger.error(message);
        return StatusUtils.error(message);
    }

    private IStatus error(String fmt, Throwable e, Object... args) {
        final String message = logger.format(fmt, args);
        logger.error(message, e);
        return StatusUtils.error(message, e);
    }


    private @Nullable BootstrapProject toBootstrapProject(org.eclipse.core.resources.IProject eclipseProject)
        throws ConfigException {
        final FileObject projectLocation = resourceService.resolve(eclipseProject);
        final IProject project = projectService.get(projectLocation);

        final ISpoofaxLanguageSpec languageSpec = languageSpecService.get(project);
        if(languageSpec == null) {
            return null;
        }

        final BootstrapProject bootstrapProject = new BootstrapProject(eclipseProject, languageSpec);
        return bootstrapProject;
    }
}
