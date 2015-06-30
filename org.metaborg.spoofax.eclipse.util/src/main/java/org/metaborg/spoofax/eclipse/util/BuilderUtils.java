package org.metaborg.spoofax.eclipse.util;

import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Ints;

public class BuilderUtils {
    private static final Logger logger = LoggerFactory.getLogger(BuilderUtils.class);

    /**
     * Returns if project contains builder with given identifier.
     * 
     * @param id
     *            Identifier of the builder to check.
     * @param project
     *            Project to check for the builder.
     * @return True if project contains the builder, false if not.
     * @throws When
     *             {@link IProject#getDescription} throws a CoreException.
     */
    public static boolean contains(String id, IProject project) throws CoreException {
        final IProjectDescription projectDesc = project.getDescription();
        final ICommand[] builders = projectDesc.getBuildSpec();
        return contains(id, builders);
    }

    /**
     * Prepends builder with given id to the build specification of given project. Does nothing if builder has already
     * been added to the project.
     * 
     * @param id
     *            Identifier of the builder to add.
     * @param project
     *            Project to add the builder to.
     * @param triggers
     *            Which triggers the builder should respond to. Only used if the builder allows its build kinds to be
     *            configured (isConfigurable="true" in plugin.xml).
     * @throws CoreException
     *             When {@link IProject#getDescription} throws a CoreException.
     * @see IncrementalProjectBuilder#FULL_BUILD
     * @see IncrementalProjectBuilder#INCREMENTAL_BUILD
     * @see IncrementalProjectBuilder#AUTO_BUILD
     * @see IncrementalProjectBuilder#CLEAN_BUILD
     */
    public static void prepend(String id, IProject project, int... triggers) throws CoreException {
        final IProjectDescription projectDesc = project.getDescription();
        final ICommand[] builders = projectDesc.getBuildSpec();
        addTo(id, project, 0, projectDesc, builders, triggers);
    }

    /**
     * Appends builder with given id to the build specification of given project. Does nothing if builder has already
     * been added to the project.
     * 
     * @param id
     *            Identifier of the builder to add.
     * @param project
     *            Project to add the builder to.
     * @param triggers
     *            Which triggers the builder should respond to. Only used if the builder allows its build kinds to be
     *            configured (isConfigurable="true" in plugin.xml).
     * @throws CoreException
     *             When {@link IProject#getDescription} throws a CoreException.
     * @see IncrementalProjectBuilder#FULL_BUILD
     * @see IncrementalProjectBuilder#INCREMENTAL_BUILD
     * @see IncrementalProjectBuilder#AUTO_BUILD
     * @see IncrementalProjectBuilder#CLEAN_BUILD
     */
    public static void append(String id, IProject project, int... triggers) throws CoreException {
        final IProjectDescription projectDesc = project.getDescription();
        final ICommand[] builders = projectDesc.getBuildSpec();
        addTo(id, project, builders.length, projectDesc, builders, triggers);
    }

    /**
     * Adds builder with {@code id} to the build specification of given project, before builders of {@code beforeId}.
     * Does nothing if builder has already been added to the project.
     * 
     * @param id
     *            Identifier of the builder to add.
     * @param project
     *            Project to add the builder to.
     * @param triggers
     *            Which triggers the builder should respond to. Only used if the builder allows its build kinds to be
     *            configured (isConfigurable="true" in plugin.xml).
     * @throws CoreException
     *             When {@link IProject#getDescription} throws a CoreException.
     * @see IncrementalProjectBuilder#FULL_BUILD
     * @see IncrementalProjectBuilder#INCREMENTAL_BUILD
     * @see IncrementalProjectBuilder#AUTO_BUILD
     * @see IncrementalProjectBuilder#CLEAN_BUILD
     */
    public static void addBefore(String id, String beforeId, IProject project, int... triggers) throws CoreException {
        final IProjectDescription projectDesc = project.getDescription();
        final ICommand[] builders = projectDesc.getBuildSpec();
        final int[] indexes = indexes(beforeId, builders);
        final int index = Ints.min(indexes);
        addTo(id, project, index, projectDesc, builders, triggers);
    }

    /**
     * Adds builder with {@code id} to the build specification of given project, after builders of {@code afterId}. Does
     * nothing if builder has already been added to the project.
     * 
     * @param id
     *            Identifier of the builder to add.
     * @param project
     *            Project to add the builder to.
     * @param triggers
     *            Which triggers the builder should respond to. Only used if the builder allows its build kinds to be
     *            configured (isConfigurable="true" in plugin.xml).
     * @throws CoreException
     *             When {@link IProject#getDescription} throws a CoreException.
     * @see IncrementalProjectBuilder#FULL_BUILD
     * @see IncrementalProjectBuilder#INCREMENTAL_BUILD
     * @see IncrementalProjectBuilder#AUTO_BUILD
     * @see IncrementalProjectBuilder#CLEAN_BUILD
     */
    public static void addAfter(String id, String afterId, IProject project, int... triggers) throws CoreException {
        final IProjectDescription projectDesc = project.getDescription();
        final ICommand[] builders = projectDesc.getBuildSpec();
        final int[] indexes = indexes(afterId, builders);
        final int index = Ints.max(indexes);
        addTo(id, project, index + 1, projectDesc, builders, triggers);
    }

    public static void addTo(String id, IProject project, int index, IProjectDescription projectDesc,
        ICommand[] builders, int... triggers) throws CoreException {
        if(!contains(id, builders)) {
            final ICommand newBuilder = projectDesc.newCommand();
            newBuilder.setBuilderName(id);
            if(triggers.length > 0) {
                if(!newBuilder.isConfigurable()) {
                    logger.error(
                        "Trying to set build triggers for {}, but builder does not support configuring triggers. "
                            + "Set isConfigurable=\"true\" for that builder in plugin.xml", id);
                }
                newBuilder.setBuilding(IncrementalProjectBuilder.AUTO_BUILD,
                    ArrayUtils.contains(triggers, IncrementalProjectBuilder.AUTO_BUILD));
                newBuilder.setBuilding(IncrementalProjectBuilder.INCREMENTAL_BUILD,
                    ArrayUtils.contains(triggers, IncrementalProjectBuilder.INCREMENTAL_BUILD));
                newBuilder.setBuilding(IncrementalProjectBuilder.FULL_BUILD,
                    ArrayUtils.contains(triggers, IncrementalProjectBuilder.FULL_BUILD));
                newBuilder.setBuilding(IncrementalProjectBuilder.CLEAN_BUILD,
                    ArrayUtils.contains(triggers, IncrementalProjectBuilder.CLEAN_BUILD));
            }
            final ICommand[] newBuilders = ArrayUtils.add(builders, index, newBuilder);
            projectDesc.setBuildSpec(newBuilders);
            project.setDescription(projectDesc, null);
        }
    }

    /**
     * Removes all builders with given identifier from given project. Does nothing if the builder has not been added to
     * the project.
     * 
     * @param id
     *            Identifier of the builder to remove.
     * @param project
     *            Project to remove the builder from.
     * @throws CoreException
     *             When {@link IProject#getDescription} or {@link IProject#setDescription} throws a CoreException.
     */
    public static void removeFrom(String id, IProject project) throws CoreException {
        final IProjectDescription projectDesc = project.getDescription();
        final ICommand[] builders = projectDesc.getBuildSpec();
        final int[] builderIndexes = indexes(id, builders);
        final ICommand[] newBuilders = ArrayUtils.removeAll(builders, builderIndexes);
        projectDesc.setBuildSpec(newBuilders);
        project.setDescription(projectDesc, null);
    }

    /**
     * Sorts builders using given sort order. Builders not part of the sort order are appended after sorted builders in
     * the original order.
     * 
     * @param project
     *            Project to check for the builder.
     * @param sortOrder
     *            Builder names that reperesent a sorting order.
     * @throws When
     *             {@link IProject#getDescription} throws a CoreException.
     */
    public static void sort(IProject project, String... sortOrder) throws CoreException {
        final IProjectDescription projectDesc = project.getDescription();
        final ICommand[] builders = projectDesc.getBuildSpec();
        final Map<String, ICommand> buildersMap = Maps.newLinkedHashMap();
        for(ICommand builder : builders) {
            buildersMap.put(builder.getBuilderName(), builder);
        }

        final Collection<ICommand> newBuilders = Lists.newArrayListWithCapacity(builders.length);
        for(String name : sortOrder) {
            final ICommand builder = buildersMap.get(name);
            if(builder != null) {
                newBuilders.add(builder);
                buildersMap.remove(name);
            }
        }

        for(ICommand builder : buildersMap.values()) {
            newBuilders.add(builder);
        }

        projectDesc.setBuildSpec(newBuilders.toArray(new ICommand[builders.length]));
        project.setDescription(projectDesc, null);
    }

    private static int[] indexes(String id, ICommand[] builders) throws CoreException {
        final Collection<Integer> indexes = Lists.newArrayList();
        for(int i = 0; i < builders.length; ++i) {
            final ICommand builder = builders[i];
            if(builder.getBuilderName().equals(id)) {
                indexes.add(i);
            }
        }
        return Ints.toArray(indexes);
    }

    private static boolean contains(String id, ICommand[] builders) throws CoreException {
        return indexes(id, builders).length != 0;
    }
}
