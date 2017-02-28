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
package me.seeber.gradle.project.base;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.SelfResolvingDependency;
import org.gradle.api.artifacts.dsl.ArtifactHandler;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.internal.ConventionMapping;
import org.gradle.api.internal.DomainObjectContext;
import org.gradle.api.internal.project.ProjectIdentifier;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.wrapper.Wrapper;
import org.gradle.internal.service.ServiceRegistry;
import org.gradle.model.Defaults;
import org.gradle.model.Each;
import org.gradle.model.Finalize;
import org.gradle.model.Model;
import org.gradle.model.ModelMap;
import org.gradle.model.Mutate;
import org.gradle.model.RuleSource;
import org.gradle.model.internal.core.Hidden;
import org.gradle.util.GradleVersion;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;

import me.seeber.gradle.plugin.AbstractProjectConfigPlugin;
import me.seeber.gradle.util.Validate;

/**
 * Plugin that applies general project configuration
 */
public class ProjectConfigPlugin extends AbstractProjectConfigPlugin {

    /**
     * Required Gradle version used for version checks
     */
    protected static final GradleVersion REQUIRED_GRADLE_VERSION = Validate.notNull(GradleVersion.version("3.4"));

    /**
     * Plugin rules
     */
    public static class PluginRules extends RuleSource {

        /**
         * Logger if we feel talkative...
         */
        private static final Logger LOGGER = Logging.getLogger(ProjectConfigPlugin.class);

        /**
         * Provide the project configuration
         *
         * @param projectConfig Project configuration
         */
        @Model
        public void projectConfig(ProjectConfig projectConfig) {
        }

        /**
         * Initialize the project configuration
         *
         * @param projectConfig Project configuration
         */
        @Defaults
        public void initializeProjectConfig(ProjectConfig projectConfig) {
            License license = projectConfig.getLicense();

            license.setExcludes(new ArrayList<>());

            VersionControl versionControl = projectConfig.getVersionControl();

            versionControl.ignore("*~");
            versionControl.ignore("/.classpath");
            versionControl.ignore("/.gradle");
            versionControl.ignore("/.project");
            versionControl.ignore("/.settings");
            versionControl.ignore("/bin/");
            versionControl.ignore("/build/");
            versionControl.ignore("/target/");
            versionControl.ignore("#*#");
        }

        /**
         * Finalize the project configuration
         *
         * <ul>
         * <li>Initialize the license URLs if not set
         * </ul>
         *
         * @param projectConfig Project configuration to finalize
         */
        @Finalize
        public void finalizeProjectConfig(ProjectConfig projectConfig) {
            License license = projectConfig.getLicense();
            String licenseId = license.getId();

            if (licenseId != null) {
                if (license.getUrl() == null) {
                    license.setUrl("https://opensource.org/licenses/" + licenseId.replace(' ', '-'));
                }

                if (license.getSourceUrl() == null) {
                    license.setSourceUrl(
                            "https://raw.githubusercontent.com/github/choosealicense.com/gh-pages/_licenses/"
                                    + licenseId.toLowerCase().replace(' ', '-') + ".txt");
                }
            }
        }

        /**
         * Validate the project configuration
         *
         * @param projectConfig Project configuration to validate
         */
        @org.gradle.model.Validate
        public void validateProjectConfig(ProjectConfig projectConfig) {
            if (!Strings.isNullOrEmpty(projectConfig.getGradleVersion())) {
                if (GradleVersion.current().compareTo(GradleVersion.version(projectConfig.getGradleVersion())) < 0) {
                    throw new IllegalArgumentException(
                            "This project requires at least Gradle " + projectConfig.getGradleVersion());
                }
            }

            Organization organization = projectConfig.getOrganization();

            if (projectConfig.getInceptionYear() == null) {
                LOGGER.warn("Please configure projectConfig.inceptionYear");
            }

            if (projectConfig.getWebsiteUrl() == null) {
                LOGGER.warn("Please configure projectConfig.websiteUrl");
            }

            if (organization.getName() == null) {
                LOGGER.warn("Please configure projectConfig.organization.name");
            }

            if (organization.getWebsiteUrl() == null) {
                LOGGER.warn("Please configure projectConfig.organization.websiteUrl");
            }

            me.seeber.gradle.project.base.License license = projectConfig.getLicense();

            if (license.getId() == null) {
                LOGGER.warn("Please configure projectConfig.license.id");
            }

            if (license.getUrl() == null) {
                LOGGER.warn("Please configure projectConfig.license.url");
            }

            if (license.getSourceUrl() == null) {
                LOGGER.warn("Please configure projectConfig.license.sourceUrl");
            }

            Repository repository = projectConfig.getRepository();

            if (repository.getName() == null) {
                LOGGER.warn("Please configure projectConfig.repository.name");
            }

            if (repository.getType() == null) {
                LOGGER.warn("Please configure projectConfig.repository.type");
            }

            if (repository.getWebsiteUrl() == null) {
                LOGGER.warn("Please configure projectConfig.repository.websiteUrl");
            }

            if (repository.getConnection() == null) {
                LOGGER.warn("Please configure projectConfig.repository.connection");
            }

            if (repository.getDeveloperConnection() == null) {
                LOGGER.warn("Please configure projectConfig.repository.developerConnection");
            }

            IssueTracker tracker = projectConfig.getIssueTracker();

            if (tracker.getId() == null) {
                LOGGER.warn("Please configure projectConfig.tracker.id");
            }

            if (tracker.getWebsiteUrl() == null) {
                LOGGER.warn("Please configure projectConfig.tracker.websiteUrl");
            }
        }

        /**
         * Provide the project as model
         *
         * @param services Service registry to look up services
         * @return Current project
         */
        @Model
        @Hidden
        public Project project(ServiceRegistry services) {
            Project project = (Project) services.get(DomainObjectContext.class);
            return project;
        }

        /**
         * Provide the artifact handler as model
         *
         * @param services Service registry to look up services
         * @return Artifact handler
         */
        @Model
        @Hidden
        public ArtifactHandler artifactHandler(ServiceRegistry services) {
            return services.get(ArtifactHandler.class);
        }

        /**
         * Provide the configurations as model
         *
         * @param services Service registry to look up services
         * @return Configurations
         */
        @Model
        @Hidden
        public ConfigurationContainer configurations(ServiceRegistry services) {
            return services.get(ConfigurationContainer.class);
        }

        /**
         * Provide the project context
         *
         * @param extensions Extension container to look up the context
         * @return Project context
         */
        @Model
        public ProjectContext projectContext(ExtensionContainer extensions) {
            return extensions.getByType(ProjectContext.class);
        }

        /**
         * Create task to update readme file from template
         *
         * @param tasks Task container to create new tasks
         * @param projectConfig Project configuration
         * @param projectContext Project context
         * @param extensions Extension container
         */
        @Mutate
        public void createReadmeUpdateTasks(ModelMap<Task> tasks, ProjectConfig projectConfig,
                ProjectContext projectContext, ExtensionContainer extensions) {
            // Create task to update the README
            Map<String, Object> context = new HashMap<>();

            extensions.getExtraProperties().getProperties().forEach((name, value) -> {
                context.put(name, value);
            });

            context.put("project", projectContext);
            context.put("projectConfig", projectConfig);

            tasks.create("readmeUpdate", Copy.class, t -> {
                t.setDescription("Update README from template.");
                t.setGroup("documentation");
                t.from("src/doc/templates");
                t.include("README.template.md");
                t.rename(n -> n.replace(".template.", "."));
                t.expand(context);

                ConventionMapping parameters = t.getConventionMapping();
                parameters.map("destinationDir", () -> projectContext.getProjectDir());
            });

            tasks.named("assemble", t -> {
                t.dependsOn("readmeUpdate");
            });
        }

        /**
         * Configure the wrapper task
         *
         * @param wrapper Wrapper task to configure
         * @param projectConfig Project configuration to get required Gradle version
         */
        @Defaults
        public void configureWrapperTask(@Each Wrapper wrapper, ProjectConfig projectConfig) {
            if (!Strings.isNullOrEmpty(projectConfig.getGradleVersion())) {
                wrapper.setGradleVersion(projectConfig.getGradleVersion());
            }
        }

        /**
         * Create debug tasks
         *
         * @param tasks Task container to create new tasks
         * @param config Project configuration
         * @param project Current project identifier
         */
        @Mutate
        public void createDebugTasks(ModelMap<Task> tasks, ProjectConfig config, ProjectIdentifier project) {
            // Create debug task to dump dependencies
            if (config.isEnableDebugTasks()) {
                tasks.create("debugDependencies", Task.class, tt -> {
                    tt.doLast(t -> {
                        PrintStream out = System.out;
                        out.print("Project: ");
                        out.println(project.getName());

                        for (Configuration configuration : t.getProject().getConfigurations()) {
                            out.print("  Configuration: ");
                            out.println(configuration.getName());

                            for (Dependency dependency : configuration.getDependencies()) {
                                out.print("    Dependency: ");
                                out.println(formatDependency(dependency));
                            }
                        }
                    });
                });
            }
        }

        /**
         * Create task to configure project
         *
         * @param tasks Task container to create task
         */
        @Mutate
        public void createConfigureTask(ModelMap<Task> tasks) {
            tasks.create("configure", Task.class, t -> {
                t.setDescription("Configure project environment");
                t.setGroup("build setup");
            });
        }

        /**
         * Format a dependency for printing
         *
         * @param dependency Dependency to format
         * @return Dependency information for printing
         */
        private String formatDependency(Dependency dependency) {
            @NonNull String info;

            if (dependency instanceof SelfResolvingDependency) {
                SelfResolvingDependency selfResolvingDependency = (SelfResolvingDependency) dependency;
                info = Validate.notNull(Joiner.on(", ").join(selfResolvingDependency.resolve()));
            }
            else {
                info = dependency.getGroup() + ":" + dependency.getName() + ":" + dependency.getVersion();
            }

            return info;
        }
    }

    /**
     * @see me.seeber.gradle.plugin.AbstractProjectConfigPlugin#initialize()
     */
    @Override
    protected void initialize() {
        checkGradleVersion();

        ProjectContext context = new ProjectContext(getProject());
        getProject().getExtensions().add("projectContext", context);

        RepositoryHandler repositories = getProject().getRepositories();
        repositories.add(repositories.mavenLocal());
        repositories.add(repositories.mavenCentral());
        repositories.add(repositories.maven(r -> r.setUrl("https://plugins.gradle.org/m2")));
        repositories.add(repositories.jcenter());

        getProject().getConfigurations().all(c -> {
            c.resolutionStrategy(s -> {
                s.preferProjectModules();
                s.cacheChangingModulesFor(0, "seconds");
            });
        });
    }

    /**
     * Check the Gradle version
     *
     * @throws GradleException if the Gradle version is smaller than the required version
     */
    protected void checkGradleVersion() {
        if (GradleVersion.current().compareTo(REQUIRED_GRADLE_VERSION) < 0) {
            throw new GradleException("Project config plugin requires at least Gradle " + REQUIRED_GRADLE_VERSION);
        }
    }
}
