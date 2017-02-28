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
package me.seeber.gradle.distribution.maven;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.component.SoftwareComponentContainer;
import org.gradle.api.internal.java.JavaLibrary;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.publish.plugins.PublishingPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.internal.service.ServiceRegistry;
import org.gradle.model.Defaults;
import org.gradle.model.Model;
import org.gradle.model.ModelMap;
import org.gradle.model.Mutate;
import org.gradle.model.RuleSource;

import me.seeber.gradle.distribution.maven.MavenConfigPlugin.PluginRules.MavenJavaRules;
import me.seeber.gradle.plugin.AbstractProjectConfigPlugin;
import me.seeber.gradle.project.base.ProjectConfig;
import me.seeber.gradle.project.base.ProjectConfigPlugin;
import me.seeber.gradle.project.base.ProjectContext;
import me.seeber.gradle.util.Names;
import me.seeber.gradle.util.Validate;

/**
 * Configure the project for Maven
 *
 * <ul>
 * <li>Apply the Maven Publish Plugin
 * <li>Create the 'install' task as a convenience alias for publishToMavenLocal'
 * <li>Create a default Maven Publication containing the 'java' component and all jars in the 'archives' configuration
 * </ul>
 *
 * @see <a href="https://docs.gradle.org/current/userguide/publishing_maven.html">Maven Publishing</a>
 */
public class MavenConfigPlugin extends AbstractProjectConfigPlugin {

    /**
     * Model rules of the plugin
     */
    public static class PluginRules extends RuleSource {

        /**
         * Provide plugin configuration
         *
         * @param mavenConfig Maven configuration
         */
        @Model
        public void mavenConfig(MavenConfig mavenConfig) {
        }

        /**
         * Create the install task from the Maven configuration
         *
         * @param tasks Task model to create task in
         */
        @Mutate
        public void createInstallTask(ModelMap<Task> tasks) {
            tasks.create("install", Task.class, t -> {
                t.setDescription("Install to local repository, convenience alias for 'publishToMavenLocal'");
                t.setGroup(PublishingPlugin.PUBLISH_TASK_GROUP);
                t.dependsOn(MavenPublishPlugin.PUBLISH_LOCAL_LIFECYCLE_TASK_NAME);
            });
        }

        /**
         * Rules for Maven Java publishing
         */
        public static class MavenJavaRules extends RuleSource {
            /**
             * Initialize the Maven configuration
             *
             * @param mavenConfig Maven configuration to initialize
             * @param projectContext Project context
             * @param configurations Configuration container
             */
            @Defaults
            public void initializeMavenConfig(MavenConfig mavenConfig, ProjectContext projectContext,
                    ConfigurationContainer configurations) {
                mavenConfig.getPublications().create(SourceSet.MAIN_SOURCE_SET_NAME, p -> {
                    p.setArtifactId(projectContext.getName());
                    p.setArchivesConfiguration(Dependency.ARCHIVES_CONFIGURATION);
                    p.setAddProjectArtifacts(false);
                    p.setCompileConfigurations(Collections.singletonList(JavaPlugin.COMPILE_CONFIGURATION_NAME));
                    p.setRuntimeConfigurations(Collections.singletonList(JavaPlugin.RUNTIME_CONFIGURATION_NAME));
                });

                if (configurations.findByName("testArchives") != null) {
                    mavenConfig.getPublications().create(SourceSet.TEST_SOURCE_SET_NAME, p -> {
                        p.setArtifactId(projectContext.getName() + "-" + SourceSet.TEST_SOURCE_SET_NAME);
                        p.setArchivesConfiguration(Names.formatName("", Dependency.ARCHIVES_CONFIGURATION,
                                SourceSet.TEST_SOURCE_SET_NAME));
                        p.setAddProjectArtifacts(true);
                        p.setCompileConfigurations(
                                Collections.singletonList(JavaPlugin.TEST_COMPILE_CONFIGURATION_NAME));
                        p.setRuntimeConfigurations(
                                Collections.singletonList(JavaPlugin.TEST_RUNTIME_CONFIGURATION_NAME));
                    });
                }
            }

            /**
             * Create the default Maven publication
             *
             * @param publishingExtension Publishing extension where the publication is created
             * @param projectContext Project context to access project information
             * @param projectConfig Project configuration
             * @param mavenConfig Maven configuration
             * @param configurations Container to access configurations
             * @param services Service manager to obtain service objects
             */
            @Mutate
            public void createPublications(PublishingExtension publishingExtension, ProjectContext projectContext,
                    ProjectConfig projectConfig, MavenConfig mavenConfig, ConfigurationContainer configurations,
                    ServiceRegistry services) {
                SoftwareComponentContainer components = services.get(SoftwareComponentContainer.class);
                JavaLibrary javaComponent = components.withType(JavaLibrary.class).getByName("java");

                for (MavenPublicationConfig publicationConfig : mavenConfig.getPublications()) {
                    Configuration configuration = configurations
                            .getByName(publicationConfig.getArchivesConfiguration());

                    publishingExtension.getPublications().create(publicationConfig.getName(), MavenPublication.class,
                            p -> {
                                p.setArtifactId(publicationConfig.getArtifactId());

                                List<@NonNull Configuration> runtimeConfigurations = publicationConfig
                                        .getRuntimeConfigurations().stream().map(c -> configurations.getByName(c))
                                        .collect(Collectors.toList());

                                List<@NonNull Configuration> compileConfigurations = publicationConfig
                                        .getCompileConfigurations().stream().map(c -> configurations.getByName(c))
                                        .collect(Collectors.toList());

                                List<@NonNull PublishArtifact> artifacts = Collections.emptyList();

                                if (publicationConfig.isAddProjectArtifacts()) {
                                    artifacts = javaComponent.getUsages().stream()
                                            .flatMap(u -> u.getArtifacts().stream()).collect(Collectors.toList());
                                }

                                PomConfigurator configurator = new PomConfigurator(projectContext, projectConfig,
                                        Validate.notNull(runtimeConfigurations),
                                        Validate.notNull(compileConfigurations), Validate.notNull(artifacts));

                                p.pom(pom -> pom.withXml(xml -> configurator.configurePom(Validate.notNull(xml))));

                                for (PublishArtifact a : configuration.getArtifacts()) {
                                    p.artifact(a);
                                }
                            });
                }
            }
        }

        /**
         * Rules for Maven Java publishing
         */
        public static class MavenJavaExtendedRules extends RuleSource {
            /**
             * Initialize the Maven configuration
             *
             * @param mavenConfig Maven configuration to initialize
             * @param projectContext Project context
             */
            @Defaults
            public void initializeMavenConfig(MavenConfig mavenConfig, ProjectContext projectContext) {
                mavenConfig.getPublications().create(SourceSet.MAIN_SOURCE_SET_NAME, p -> {
                    p.setArtifactId(projectContext.getName());
                    p.setArchivesConfiguration(Dependency.ARCHIVES_CONFIGURATION);
                    p.setAddProjectArtifacts(false);
                    p.setCompileConfigurations(Collections.singletonList(JavaPlugin.COMPILE_CONFIGURATION_NAME));
                    p.setRuntimeConfigurations(Collections.singletonList(JavaPlugin.RUNTIME_CONFIGURATION_NAME));
                });

                mavenConfig.getPublications().create(SourceSet.TEST_SOURCE_SET_NAME, p -> {
                    p.setArtifactId(projectContext.getName() + "-" + SourceSet.TEST_SOURCE_SET_NAME);
                    p.setArchivesConfiguration(
                            Names.formatName("", Dependency.ARCHIVES_CONFIGURATION, SourceSet.TEST_SOURCE_SET_NAME));
                    p.setAddProjectArtifacts(true);
                    p.setCompileConfigurations(Collections.singletonList(JavaPlugin.TEST_COMPILE_CONFIGURATION_NAME));
                    p.setRuntimeConfigurations(Collections.singletonList(JavaPlugin.TEST_RUNTIME_CONFIGURATION_NAME));
                });
            }
        }

    }

    /**
     * @see me.seeber.gradle.plugin.AbstractProjectConfigPlugin#initialize()
     */
    @Override
    public void initialize() {
        getProject().getPlugins().apply(ProjectConfigPlugin.class);
        getProject().getPlugins().apply(MavenPublishPlugin.class);

        getProject().getPluginManager().withPlugin("java", p -> {
            getProject().getPluginManager().apply(MavenJavaRules.class);
        });
    }
}
