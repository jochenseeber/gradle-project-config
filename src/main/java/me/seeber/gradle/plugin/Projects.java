/**
 * BSD 2-Clause License
 *
 * Copyright (c) 2016-2017, Jochen Seeber
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package me.seeber.gradle.plugin;

import java.io.File;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.internal.artifacts.publish.ArchivePublishArtifact;
import org.gradle.api.internal.artifacts.publish.DefaultPublishArtifact;
import org.gradle.api.plugins.BasePluginConvention;
import org.gradle.api.tasks.bundling.Jar;

import com.google.common.base.Joiner;

import me.seeber.gradle.util.Validate;

/**
 * Utilitiy methods for {@link Project} objects
 */
public abstract class Projects {

    /**
     * Joiner used to join artifact info in file names
     */
    protected static final Joiner FILE_PART_JOINER = Validate.notNull(Joiner.on("-").skipNulls());

    /**
     * Create a {@link PublishArtifact} for a jar created by a {@link Jar} task
     *
     * We use this instead of {@link ArchivePublishArtifact} because we can add the unresolved task dependency by name,
     * thus avoiding a direct dependency on the task.
     *
     * @param project Project to create artifact for
     * @param name Name of the new artifact
     * @param classifier Classifier for new artifact
     * @param type Type of new artifact
     * @param extension Extension of new artifact
     * @param producingTask Task that produces the artifact
     * @return Publish artifact for the jar
     */
    public static PublishArtifact createJarPublishArtifact(Project project, String name, @Nullable String classifier,
            String type, String extension, Jar producingTask) {
        BasePluginConvention baseConvention = project.getConvention().getPlugin(BasePluginConvention.class);
        String jarName = FILE_PART_JOINER.join(name, project.getVersion(), classifier) + "." + extension;
        File jarFile = baseConvention.getLibsDir().toPath().resolve(jarName).toFile();
        DefaultPublishArtifact artifact = new DefaultPublishArtifact(name, extension, type, classifier, null, jarFile,
                producingTask);
        return artifact;
    }

    /**
     * Holds a project element in addition to the element's project and configuration
     *
     * This class is used to return an element together with its owners when searching.
     *
     * @param <T> Type of the project element
     */
    public static class ProjectElement<T> {

        /**
         * Project the element belongs to
         */
        private final Project project;

        /**
         * Configuration the element belongs to
         */
        private final Configuration configuration;

        /**
         * Element
         */
        private final T element;

        /**
         * Create a new project element
         *
         * @param project Project the element belongs to
         * @param configuration Configuration the element belongs to
         * @param element Referenced project element
         */
        public ProjectElement(Project project, Configuration configuration, T element) {
            this.project = project;
            this.configuration = configuration;
            this.element = element;
        }

        /**
         * Get the project the element belongs to
         *
         * @return Project the element belongs to
         */
        public Project getProject() {
            return this.project;
        }

        /**
         * Get the configuration the element belongs to
         *
         * @return Configuration the element belongs to
         */
        public Configuration getConfiguration() {
            return this.configuration;
        }

        /**
         * Get the project element
         *
         * @return Project element
         */
        public T getElement() {
            return this.element;
        }

    }

    /**
     * Find a resolved artifact in a collection of projects
     *
     * @param projects Projects to search
     * @param configurationPredicate Predicate to test configurations
     * @param artifactPredicate Predicate to test artifacts
     * @return Found element or empty value
     */
    public static Optional<ProjectElement<ResolvedArtifact>> findResolvedArtifact(Collection<@NonNull Project> projects,
            Predicate<@NonNull Configuration> configurationPredicate,
            Predicate<@NonNull ResolvedArtifact> artifactPredicate) {
        Optional<ProjectElement<ResolvedArtifact>> info = Optional.empty();

        for (Project project : projects) {
            info = findResolvedArtifact(project, configurationPredicate, artifactPredicate);

            if (info.isPresent()) {
                break;
            }
        }

        return info;
    }

    /**
     * Find a resolved artifact in a project
     *
     * @param project Project to search
     * @param configurationPredicate Predicate to test configurations
     * @param artifactPredicate Predicate to test artifacts
     * @return Found element or empty value
     */
    public static Optional<ProjectElement<ResolvedArtifact>> findResolvedArtifact(Project project,
            Predicate<@NonNull Configuration> configurationPredicate,
            Predicate<@NonNull ResolvedArtifact> artifactPredicate) {
        Optional<ProjectElement<ResolvedArtifact>> info = Optional.empty();

        for (@NonNull Configuration configuration : project.getConfigurations()) {
            if (configurationPredicate.test(configuration)) {
                info = findResolvedArtifact(project, configuration, artifactPredicate);

                if (info.isPresent()) {
                    break;
                }
            }
        }

        return info;
    }

    /**
     * Find a resolved artifact in a configuration
     *
     * @param project Project to search
     * @param configuration Configuration to search
     * @param artifactPredicate Predicate to test artifacts
     * @return Found element or empty value
     */
    public static Optional<ProjectElement<ResolvedArtifact>> findResolvedArtifact(Project project,
            Configuration configuration, Predicate<@NonNull ResolvedArtifact> artifactPredicate) {
        Optional<ProjectElement<ResolvedArtifact>> info = Optional.empty();

        for (@NonNull ResolvedArtifact artifact : configuration.getResolvedConfiguration().getResolvedArtifacts()) {
            if (artifactPredicate.test(artifact)) {
                info = Optional.of(new ProjectElement<>(project, configuration, artifact));
                break;
            }
        }

        return info;
    }

    /**
     * Search a collection of projects for a publish artifact
     *
     * @param projects Projects to search
     * @param configurationPredicate Predicate to test configurations
     * @param artifactPredicate Predicate to test artifacts
     * @return Found element or empty value
     */
    public static Optional<ProjectElement<PublishArtifact>> findPublishArtifact(Collection<@NonNull Project> projects,
            Predicate<@NonNull Configuration> configurationPredicate,
            Predicate<@NonNull PublishArtifact> artifactPredicate) {
        Optional<ProjectElement<PublishArtifact>> info = Optional.empty();

        for (Project project : projects) {
            info = findPublishArtifact(project, configurationPredicate, artifactPredicate);

            if (info.isPresent()) {
                break;
            }
        }

        return info;
    }

    /**
     * Search a project for a publish artifact
     *
     * @param project Project to search
     * @param configurationPredicate Predicate to test configurations
     * @param artifactPredicate Predicate to test artifacts
     * @return Found element or empty value
     */
    public static Optional<ProjectElement<PublishArtifact>> findPublishArtifact(Project project,
            Predicate<@NonNull Configuration> configurationPredicate,
            Predicate<@NonNull PublishArtifact> artifactPredicate) {
        Optional<ProjectElement<PublishArtifact>> info = Optional.empty();

        for (@NonNull Configuration configuration : project.getConfigurations()) {
            if (configurationPredicate.test(configuration)) {
                info = findPublishArtifact(project, configuration, artifactPredicate);

                if (info.isPresent()) {
                    break;
                }
            }
        }

        return info;
    }

    /**
     * Search a configuration for a publish artifact
     *
     * @param project Project to search
     * @param configuration Configuration to search
     * @param artifactPredicate Predicate to test artifacts
     * @return Found element or empty value
     */
    public static Optional<ProjectElement<PublishArtifact>> findPublishArtifact(Project project,
            Configuration configuration, Predicate<@NonNull PublishArtifact> artifactPredicate) {
        Optional<ProjectElement<PublishArtifact>> info = Optional.empty();

        for (@NonNull PublishArtifact artifact : configuration.getAllArtifacts()) {
            if (artifactPredicate.test(artifact)) {
                info = Optional.of(new ProjectElement<>(project, configuration, artifact));
                break;
            }
        }

        return info;
    }
}
