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

package me.seeber.gradle.validation.checkstyle;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.internal.file.FileOperations;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.quality.Checkstyle;
import org.gradle.api.plugins.quality.CheckstyleExtension;
import org.gradle.api.plugins.quality.CheckstylePlugin;
import org.gradle.language.base.ProjectSourceSet;
import org.gradle.language.java.JavaSourceSet;
import org.gradle.model.Defaults;
import org.gradle.model.Model;
import org.gradle.model.ModelMap;
import org.gradle.model.Mutate;
import org.gradle.model.RuleSource;
import org.gradle.plugins.ide.eclipse.EclipsePlugin;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;
import org.gradle.plugins.ide.eclipse.model.EclipseProject;

import me.seeber.gradle.plugin.AbstractProjectConfigPlugin;
import me.seeber.gradle.project.base.ProjectConfig;
import me.seeber.gradle.project.base.ProjectConfigPlugin;
import me.seeber.gradle.project.base.VersionControl;
import me.seeber.gradle.util.Classes;
import me.seeber.gradle.util.Tasks;
import me.seeber.gradle.util.Validate;
import me.seeber.gradle.validation.checkstyle.CheckstyleConfigPlugin.PluginRules.EclipseJavaRules;

/**
 * Gradle plugin project configuration
 */
public class CheckstyleConfigPlugin extends AbstractProjectConfigPlugin {

    /**
     * Pattern to identify checkstyle task prefix
     */
    protected static final Pattern CHECKSTYLE_TASK_PREFIX = Pattern.compile("^checkstyle");

    /**
     * Name of main config update task
     */
    protected static final String CONFIGURE_CHECKSTYLE_TASK_NAME = "configureCheckstyle";

    /**
     * Name of the Eclipse Checkstyle configuration task
     */
    protected static final String ECLIPSE_CHECKSTYLE_TASK_NAME = "eclipseCheckstyle";

    /**
     * Eclipse checkstyle nature
     */
    protected static final String CHECKSTYLE_NATURE = "net.sf.eclipsecs.core.CheckstyleNature";

    /**
     * Plugin rules
     */
    public static class PluginRules extends RuleSource {

        /**
         * Get the Checkstyle configuration
         *
         * @param checkstyleConfig Checkstyle configuration
         */
        @Model
        public void checkstyleConfig(CheckstyleConfig checkstyleConfig) {
            checkstyleConfig.setIgnoreSourceSets(new HashSet<>());
        }

        /**
         * Initialize the project configuration
         *
         * <ul>
         * <li>Add the checkstyle config files to the files ignored by version control
         * </ul>
         *
         * @param projectConfig Project configuration to update
         * @param checkstyleConfig Checkstyle configuration
         * @param sources Source sets
         * @param files File operations object to resolve file names
         */
        @Defaults
        public void initializeProjectConfig(ProjectConfig projectConfig, CheckstyleConfig checkstyleConfig,
                ProjectSourceSet sources, FileOperations files) {
            VersionControl versionControl = projectConfig.getVersionControl();

            for (JavaSourceSet source : sources.withType(JavaSourceSet.class)) {
                if (!checkstyleConfig.getIgnoreSourceSets().contains(source.getParentName())) {
                    File configFile = getCheckstyleConfigFile(source.getParentName(), files);
                    versionControl.ignore("/" + Validate.notNull(files.relativePath(configFile)));
                }
            }
        }

        /**
         * Create Checkstyle task
         *
         * @param tasks Task container
         * @param checkstyleConfig Checkstyle configuration
         * @param sources Project sources to run checkstyle on
         */
        @Mutate
        public void createCheckstyleTask(ModelMap<Task> tasks, CheckstyleConfig checkstyleConfig,
                ProjectSourceSet sources) {
            tasks.create("checkstyle", t -> {
                t.setDescription("Run Checkstyle on all source sets");
                t.setGroup(JavaBasePlugin.VERIFICATION_GROUP);

                for (JavaSourceSet source : sources.withType(JavaSourceSet.class)) {
                    if (!checkstyleConfig.getIgnoreSourceSets().contains(source.getParentName())) {
                        t.dependsOn(getCheckstyleTaskName(source));
                    }
                }
            });
        }

        /**
         * Create Checkstyle tasks
         *
         * @param tasks Task container
         * @param checkstyleConfig Checkstyle configuration
         * @param sources Source sets
         * @param files
         */
        @Mutate
        public void createCheckstyleTasks(ModelMap<Checkstyle> tasks, CheckstyleConfig checkstyleConfig,
                ProjectSourceSet sources, FileOperations files) {
            for (JavaSourceSet source : sources.withType(JavaSourceSet.class)) {
                String taskName = getCheckstyleTaskName(source);

                if (!checkstyleConfig.getIgnoreSourceSets().contains(source.getParentName())) {
                    Checkstyle task = tasks.get(taskName);

                    if (task != null) {
                        File checkstyleConfigFile = getCheckstyleConfigFile(source.getParentName(), files);

                        task.setGroup(JavaBasePlugin.VERIFICATION_GROUP);

                        if (checkstyleConfigFile.exists()) {
                            task.setConfigFile(checkstyleConfigFile);
                        }

                        if (task.getConfigFile() != null && task.getConfigFile().getParentFile() != null) {
                            task.getConfigProperties().putIfAbsent("config_loc", task.getConfigFile().getParent());
                        }
                    }
                }
            }
        }

        /**
         * Create tasks to update Checkstyle configuration
         *
         * @param tasks Task container
         * @param sources Source sets to add tasks for
         * @param files File operations object to resolve file names
         */
        @Mutate
        public void createUpdateTasks(ModelMap<Task> tasks, ProjectSourceSet sources, FileOperations files) {
            tasks.create(CONFIGURE_CHECKSTYLE_TASK_NAME, Task.class, t -> {
                t.setDescription("Update checkstyle configuration");
                t.setGroup("Build Setup");
            });

            for (JavaSourceSet source : sources.withType(JavaSourceSet.class)) {
                File configFile = getCheckstyleConfigFile(source.getParentName(), files);
                String resourceName = "checkstyle_" + source.getParentName() + ".xml";

                String config = Classes.getResourceString(CheckstyleConfigPlugin.class, resourceName).orElseGet(
                        () -> Classes.getResourceString(CheckstyleConfigPlugin.class, "checkstyle.xml").get());

                String taskName = getUpdateConfigTaskName(source);

                tasks.create(taskName, CheckstyleConfigUpdate.class, t -> {
                    t.setDescription(String.format("Update checkstyle configuration for source set '%s'",
                            source.getParentName()));
                    t.setGroup("Build Setup");
                    t.setConfig(config);
                    t.setConfigFile(configFile);
                });

                tasks.named(getCheckstyleTaskName(source), t -> {
                    t.dependsOn(taskName);
                });

                tasks.named(CONFIGURE_CHECKSTYLE_TASK_NAME, t -> {
                    t.dependsOn(taskName);
                });
            }
        }

        /**
         * Get the task name for a config update task
         *
         * @param sources Java source set
         * @return Task name
         */
        private String getUpdateConfigTaskName(JavaSourceSet sources) {
            return Tasks.taskName(CONFIGURE_CHECKSTYLE_TASK_NAME, Validate.notNull(sources.getParentName()), "");
        }

        /**
         * Get the task name for a config update task
         *
         * @param sources Java source set
         * @return Task name
         */
        private String getCheckstyleTaskName(JavaSourceSet sources) {
            return Tasks.taskName("checkstyle", Validate.notNull(sources.getParentName()), "");
        }

        /**
         * Additional rules for Java projects
         */
        public static class EclipseJavaRules extends RuleSource {
            /**
             * Create eclipse tasks to generate Checkstyle settings
             *
             * @param tasks Task container to create tasks
             * @param sources Source sets
             * @param files File operations object to resolve file names
             */
            @Mutate
            public void createEclipseCheckstyleTask(ModelMap<Task> tasks, ProjectSourceSet sources,
                    FileOperations files) {
                List<String> sourceNames = sources.withType(JavaSourceSet.class).stream().map(s -> s.getParentName())
                        .collect(Collectors.toList());

                tasks.create(ECLIPSE_CHECKSTYLE_TASK_NAME, GenerateEclipseCheckstyle.class, t -> {
                    t.setDescription("Generates Checkstyle configuration for Eclipse.");
                    t.setGroup("IDE");
                    t.setSourceSets(sourceNames);
                    t.setSettingsFile(files.file(".checkstyle"));
                });
            }

            /**
             * Configure the 'eclipse' task
             *
             * @param tasks Task container to manage tasks
             * @param eclipseModel Eclipse configuration to create dependency
             */
            @Mutate
            public void configureEclipseTasks(ModelMap<Task> tasks, EclipseModel eclipseModel) {
                tasks.named(EclipsePlugin.ECLIPSE_TASK_NAME, t -> {
                    t.dependsOn(ECLIPSE_CHECKSTYLE_TASK_NAME);
                });

                tasks.named(Tasks.cleanName(Validate.notNull(EclipsePlugin.ECLIPSE_TASK_NAME)), t -> {
                    t.dependsOn(Tasks.cleanName(ECLIPSE_CHECKSTYLE_TASK_NAME));
                });
            }

            /**
             * Configure Eclipse project
             *
             * @param eclipseModel Eclipse model to configure
             */
            @Mutate
            public void configureEclipseProject(EclipseModel eclipseModel) {
                EclipseProject eclipseProject = eclipseModel.getProject();
                eclipseProject.natures(CHECKSTYLE_NATURE);
            }
        }
    }

    /**
     * @see me.seeber.gradle.plugin.AbstractProjectConfigPlugin#initialize()
     */
    @Override
    public void initialize() {
        getProject().getPluginManager().apply(ProjectConfigPlugin.class);
        getProject().getPluginManager().apply(CheckstylePlugin.class);

        getProject().getPluginManager().withPlugin("java", p -> {
            applyEclipseRules(getProject());
        });

        getProject().getPluginManager().withPlugin("eclipse", p -> {
            applyEclipseRules(getProject());
        });

        CheckstyleExtension checkstyleConfig = getProject().getExtensions().getByType(CheckstyleExtension.class);
        checkstyleConfig.setToolVersion("7.3");
    }

    /**
     * Apply the Checkstyle rules for Eclipse Java projects
     *
     * @param project Project to apply rules to
     */
    protected void applyEclipseRules(Project project) {
        if (project.getPluginManager().hasPlugin("java") && project.getPluginManager().hasPlugin("eclipse")) {
            getProject().getPluginManager().apply(EclipseJavaRules.class);
        }
    }

    /**
     * Get the checkstyle configuration file for a source set
     *
     * @param sourceName Source set name
     * @param files FIle operations object to resolve file names
     * @return Checkstyle config file
     */
    public static File getCheckstyleConfigFile(String sourceName, FileOperations files) {
        File configFile = files.file("config/" + sourceName + "/java/checkstyle.xml");
        return configFile;
    }

}
