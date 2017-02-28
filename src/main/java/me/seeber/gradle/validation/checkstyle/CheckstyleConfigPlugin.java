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
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.internal.file.FileOperations;
import org.gradle.api.plugins.quality.Checkstyle;
import org.gradle.api.plugins.quality.CheckstyleExtension;
import org.gradle.api.plugins.quality.CheckstylePlugin;
import org.gradle.language.base.ProjectSourceSet;
import org.gradle.language.base.plugins.LifecycleBasePlugin;
import org.gradle.language.java.JavaSourceSet;
import org.gradle.model.Defaults;
import org.gradle.model.Each;
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
import me.seeber.gradle.util.Tasks.AdjustCase;
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
         * Initialize the project configuration
         *
         * <ul>
         * <li>Add the checkstyle config files to the files ignored by version control
         * </ul>
         *
         * @param projectConfig Project configuration to update
         * @param sources Source sets
         * @param files File operations object to resolve file names
         */
        @Defaults
        public void initializeProjectConfig(ProjectConfig projectConfig, ProjectSourceSet sources,
                FileOperations files) {
            VersionControl versionControl = projectConfig.getVersionControl();

            for (JavaSourceSet source : sources.withType(JavaSourceSet.class)) {
                File configFile = getCheckstyleConfigFile(source, files);
                versionControl.ignore("/" + Validate.notNull(files.relativePath(configFile)));
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

            tasks.create(Tasks.cleanName(CONFIGURE_CHECKSTYLE_TASK_NAME));

            tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME, t -> {
                t.dependsOn(CONFIGURE_CHECKSTYLE_TASK_NAME);
            });

            for (JavaSourceSet source : sources.withType(JavaSourceSet.class)) {
                File configFile = getCheckstyleConfigFile(source, files);

                String config = Classes.getResourceString(CheckstyleConfigPlugin.class, configFile.getName()).orElseGet(
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

                tasks.named(Tasks.cleanName(CONFIGURE_CHECKSTYLE_TASK_NAME), t -> {
                    t.dependsOn(Tasks.cleanName(taskName));
                });
            }
        }

        /**
         * Configure each Checkstyle task
         *
         * @param task Checkstyle task to configure
         * @param sources Source sets to get source directories
         * @param files File operations object to resolve file names
         */
        @Defaults
        public void configureCheckstyleTask(@Each Checkstyle task, ProjectSourceSet sources, FileOperations files) {
            String sourceSetName = Tasks.namePart(CHECKSTYLE_TASK_PREFIX.matcher(task.getName()).replaceAll(""),
                    AdjustCase.LOWER);

            Optional<JavaSourceSet> source = sources.withType(JavaSourceSet.class).stream()
                    .filter(s -> s.getParentName().equals(sourceSetName)).findAny();

            source.ifPresent(s -> {
                File checkstyleConfigFile = getCheckstyleConfigFile(Validate.notNull(s), files);

                if (checkstyleConfigFile.exists()) {
                    task.setConfigFile(checkstyleConfigFile);
                }
            });

            if (task.getConfigFile() != null && task.getConfigFile().getParentFile() != null) {
                task.getConfigProperties().putIfAbsent("config_loc", task.getConfigFile().getParent());
            }
        }

        /**
         * Get the checkstyle configuration file for a source set
         *
         * @param source Java source set
         * @param files FIle operations object to resolve file names
         * @return Checkstyle config file
         */
        private File getCheckstyleConfigFile(JavaSourceSet source, FileOperations files) {
            File sourceDir = source.getSource().getSrcDirs().iterator().next();
            File configFile = sourceDir.toPath().resolve("../checkstyle/checkstyle.xml").normalize().toFile();
            return configFile;
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
}
