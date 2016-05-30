package org.metaborg.spoofax.eclipse.meta.bootstrap;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

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
import org.metaborg.core.language.LanguageIdentifier;
import org.metaborg.core.language.LanguageVersion;
import org.metaborg.core.project.IProject;
import org.metaborg.core.project.IProjectService;
import org.metaborg.spoofax.eclipse.meta.SpoofaxMetaPlugin;
import org.metaborg.spoofax.eclipse.meta.build.BuilderConfig;
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
import org.metaborg.util.collection.UniqueQueue;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;

public class BootstrapJob extends Job {
    private static final ILogger logger = LoggerUtils.logger(BootstrapJob.class);

    private static final int maxIterations = 100;


    private final IEclipseResourceService resourceService;
    private final IProjectService projectService;
    private final ILanguageDiscoveryService languageDiscoveryService;

    private final Provider<ISpoofaxLanguageSpecConfigBuilder> languageSpecConfigBuilderProvider;
    private final ISpoofaxLanguageSpecConfigWriter languageSpecConfigWriter;
    private final ISpoofaxLanguageSpecService languageSpecService;

    private final IWorkspaceRoot workspaceRoot;
    private final org.eclipse.core.resources.IProject targetEclipseProject;


    @Inject public BootstrapJob(IEclipseResourceService resourceService, IProjectService projectService,
        ILanguageDiscoveryService languageDiscoveryService,
        Provider<ISpoofaxLanguageSpecConfigBuilder> languageSpecConfigBuilderProvider,
        ISpoofaxLanguageSpecConfigWriter languageSpecConfigWriter, ISpoofaxLanguageSpecService languageSpecService,
        @Assisted IWorkspaceRoot workspaceRoot, @Assisted org.eclipse.core.resources.IProject targetEclipseProject) {
        super("Bootstrapping " + targetEclipseProject);
        this.resourceService = resourceService;
        this.projectService = projectService;
        this.languageDiscoveryService = languageDiscoveryService;

        this.languageSpecConfigBuilderProvider = languageSpecConfigBuilderProvider;
        this.languageSpecConfigWriter = languageSpecConfigWriter;
        this.languageSpecService = languageSpecService;

        this.workspaceRoot = workspaceRoot;
        this.targetEclipseProject = targetEclipseProject;
    }


    @Override protected IStatus run(IProgressMonitor rootMonitor) {
        final SubMonitor monitor = SubMonitor.convert(rootMonitor);
        monitor.setWorkRemaining(4);


        // Gather all projects
        if(monitor.isCanceled()) {
            return StatusUtils.cancel();
        }
        monitor.setTaskName("Gathering language specification projects");
        final Map<String, BootstrapProject> projects;
        try {
            projects = projects(workspaceRoot.getProjects());
        } catch(ConfigException | CoreException e) {
            return error("Cannot gather bootstrapping projects", e);
        }
        monitor.worked(1);


        // Check target project
        if(monitor.isCanceled()) {
            return StatusUtils.cancel();
        }
        monitor.setTaskName("Checking target project");
        final BootstrapProject targetProject;
        try {
            targetProject = targetProject(projects);
        } catch(ConfigException | CoreException e) {
            return error("Cannot get target project", e);
        }
        monitor.worked(1);


        // Set up dependency graph
        if(monitor.isCanceled()) {
            return StatusUtils.cancel();
        }
        monitor.setTaskName("Creating dependency graph");
        final BiSetMultimap<BootstrapProject, BootstrapProject> deps;
        try {
            deps = deps(projects, monitor);
        } catch(CoreException e) {
            return error("Cannot get dependencies", e);
        }
        final BiSetMultimap<BootstrapProject, BootstrapProject> transDeps = allTansDeps(deps);
        final Set<BootstrapProject> targetTransDependants = dependents(transDeps, targetProject, true);
        monitor.worked(1);


        // Fixpoint
        if(monitor.isCanceled()) {
            return StatusUtils.cancel();
        }
        monitor.setTaskName("Bootstrapping fixpoint");
        logger.info("Starting bootstrapping fixpoint");
        try {
            final SubMonitor fixpointMonitor = monitor.newChild(1);
            fixpointMonitor.setWorkRemaining(maxIterations * 2);
            int iteration = 1;
            while(iteration <= maxIterations) {
                logger.info("Fixpoint iteration {}", iteration);
                if(monitor.isCanceled()) {
                    return StatusUtils.cancel();
                }

                boolean fixpoint = true;
                for(BootstrapProject project : targetTransDependants) {
                    logger.info("Boostrapping {}", project);
                    final SubMonitor projectMonitor = fixpointMonitor.newChild(2);
                    if(monitor.isCanceled()) {
                        return StatusUtils.cancel();
                    }

                    final FileObject prevBinary = project.binary();

                    final LanguageVersion nextVersion;
                    if(prevBinary == null) {
                        // No previous binary available, so has not been bootstrapped yet.
                        logger.info("First build for {}", project);

                        // Set version of the project to the next version.
                        nextVersion = nextVersion(project);
                        setVersion(project, nextVersion);
                    } else {
                        nextVersion = null;
                    }

                    try {
                        // Build
                        final FileObject binary = build(project, projectMonitor.newChild(1));

                        if(prevBinary == null) {
                            // Set dependencies to this project to the next version.
                            setDependencyVersions(projects.values(), project.identifier().groupId,
                                project.identifier().id, nextVersion);

                            // Don't reach fixpoint if there was no previous binary for a project.
                            logger.warn("No previous binary to compare with, another fixpoint iteration is required");
                            fixpoint = false;
                        } else {
                            // Compare binaries. Don't reach fixpoint if the new binary is not stable.
                            // Compare on left side of logical and operation to avoid short circuiting.
                            fixpoint = compare(prevBinary, binary) && fixpoint;
                        }

                        // Store binary and reload after comparison, to avoid overwriting existing stored binary.
                        final FileObject storedBinary = storeBinary(project, binary);
                        try {
                            reloadLanguage(storedBinary);
                            project.updateBinary(storedBinary);
                        } catch(MetaborgException e) {
                            // Don't reach fixpoint if reloading language failed.
                            logger.error("Reloading language for {} failed", e, project);
                            logger.warn("Reloading failed, another fixpoint iteration is required");
                            fixpoint = false;
                        }
                    } catch(CoreException e) {
                        // Don't reach fixpoint if build failed.
                        logger.error("Building language specification for {} failed", e, project);
                        logger.warn("Build failed, another fixpoint iteration is required");
                        fixpoint = false;
                    }


                    projectMonitor.worked(1);
                    projectMonitor.done();
                }
                if(fixpoint) {
                    logger.info("Fixpoint was reached");
                    break;
                }
                ++iteration;
            }
            fixpointMonitor.done();
        } catch(IOException | ConfigException e) {
            return error("Cleaning, building, or reloading a project failed unexpectedly", e);
        }


        monitor.done();
        return StatusUtils.success();
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


    private Map<String, BootstrapProject> projects(org.eclipse.core.resources.IProject[] eclipseProjects)
        throws CoreException, ConfigException {
        final Map<String, BootstrapProject> projects = Maps.newHashMap();
        for(org.eclipse.core.resources.IProject eclipseProject : eclipseProjects) {
            if(!eclipseProject.isOpen()) {
                continue;
            }

            final BootstrapProject bootstrapProject = toBootstrapProject(eclipseProject);
            if(bootstrapProject == null) {
                // Project is not a language specification, skip.
                continue;
            }
            final String id = bootstrapProject.idNoVersion();

            if(projects.containsKey(id)) {
                throw new CoreException(error("Project {} already exists", bootstrapProject));
            }

            logger.info("Adding project {}", bootstrapProject);
            projects.put(id, bootstrapProject);
        }
        return projects;
    }

    private BootstrapProject targetProject(Map<String, BootstrapProject> projects)
        throws ConfigException, CoreException {
        final BootstrapProject targetProject;
        final BootstrapProject tempTargetProject = toBootstrapProject(targetEclipseProject);
        if(tempTargetProject == null) {
            throw new CoreException(
                error("Target project {} is not a language specification project", targetEclipseProject));
        }
        if(!projects.containsValue(tempTargetProject)) {
            throw new CoreException(error("Target project {} was not found in the workspace", tempTargetProject));
        }
        // Get target project from projects map, such that the instance is shared.
        // This is important because the language specification inside a bootstrap project can be updated later.
        targetProject = projects.get(tempTargetProject.idNoVersion());
        return targetProject;
    }


    private BiSetMultimap<BootstrapProject, BootstrapProject> deps(Map<String, BootstrapProject> projects,
        SubMonitor monitor) throws CoreException {
        final BiSetMultimap<BootstrapProject, BootstrapProject> dependencies = new BiLinkedHashMultimap<>();
        for(BootstrapProject project : projects.values()) {
            if(monitor.isCanceled()) {
                throw new CoreException(StatusUtils.cancel());
            }
            final ISpoofaxLanguageSpecConfig config = project.config();
            for(LanguageIdentifier dep : Iterables.concat(config.compileDeps(), config.sourceDeps())) {
                if(monitor.isCanceled()) {
                    throw new CoreException(StatusUtils.cancel());
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
        return dependencies;
    }

    private Set<BootstrapProject> oneTansDeps(BiSetMultimap<BootstrapProject, BootstrapProject> deps,
        BootstrapProject project) {
        final Set<BootstrapProject> transDeps = Sets.newHashSet();
        final Queue<BootstrapProject> todo = new UniqueQueue<>();
        todo.addAll(deps.get(project));
        while(!todo.isEmpty()) {
            final BootstrapProject dep = todo.poll();
            transDeps.add(dep);
            for(BootstrapProject transDep : deps.get(dep)) {
                if(!transDeps.contains(transDep)) {
                    todo.add(transDep);
                }
            }
        }
        return transDeps;
    }

    private BiSetMultimap<BootstrapProject, BootstrapProject>
        allTansDeps(BiSetMultimap<BootstrapProject, BootstrapProject> deps) {
        final BiSetMultimap<BootstrapProject, BootstrapProject> allTransDeps = new BiLinkedHashMultimap<>();
        for(BootstrapProject dep : deps.values()) {
            final Set<BootstrapProject> transDeps = oneTansDeps(deps, dep);
            allTransDeps.putAll(dep, transDeps);
        }
        return allTransDeps;
    }

    private Set<BootstrapProject> dependents(BiSetMultimap<BootstrapProject, BootstrapProject> transDeps,
        BootstrapProject project, boolean includeSelf) {
        Set<BootstrapProject> dependents = transDeps.getInverse(project);
        if(includeSelf) {
            dependents = Sets.newHashSet(dependents);
            dependents.add(project);
        }
        return dependents;
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
        final org.eclipse.core.resources.IProject eclipseProject = project.eclipseProject;
        final BuilderConfig config = new BuilderConfig(eclipseProject, false, true, false);
        logger.info("Cleaning {}", project);
        eclipseProject.build(config, IncrementalProjectBuilder.CLEAN_BUILD, monitor.newChild(1));
        logger.info("Building {}", project);
        eclipseProject.build(config, IncrementalProjectBuilder.FULL_BUILD, monitor.newChild(1));
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
        final ResourceComparer comparer = new ResourceComparer(resourceService);
        final Collection<ResourceDiff> diffs = comparer.compare(leftBinary, rightBinary);
        if(diffs.isEmpty()) {
            logger.info("Binaries are identical", leftBinary, rightBinary);
            return true;
        }
        logger.warn("Binaries are different", leftBinary, rightBinary);
        for(ResourceDiff diff : diffs) {
            logger.warn(diff.toString());
        }
        return false;
    }

    private Iterable<ILanguageComponent> reloadLanguage(FileObject binary) throws MetaborgException {
        final FileObject zipBinary = resourceService.resolve("zip:" + binary.getName().getURI() + "!/");
        logger.info("Reloading language implementation at {}", zipBinary);
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
}
