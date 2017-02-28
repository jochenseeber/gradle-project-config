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
package me.seeber.gradle.project.java;

import java.util.List;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.plugins.BasePluginConvention;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.language.base.ProjectSourceSet;
import org.gradle.language.java.JavaSourceSet;
import org.gradle.model.Defaults;
import org.gradle.model.Each;
import org.gradle.model.Model;
import org.gradle.model.Mutate;
import org.gradle.model.Path;
import org.gradle.model.RuleSource;
import org.gradle.model.internal.core.Hidden;

import com.google.common.collect.ImmutableList;

import me.seeber.gradle.plugin.AbstractProjectConfigPlugin;
import me.seeber.gradle.plugin.Projects;
import me.seeber.gradle.project.base.ProjectConfigPlugin;
import me.seeber.gradle.util.Validate;

/**
 * Java project configuration
 */
public class JavaConfigPlugin extends AbstractProjectConfigPlugin {

    /**
     * Name of the configuration for external Eclipse annotations
     */
    public static final String ANNOTATIONS_CONFIGURATION = "annotations";

    /**
     * Plugin rules
     */
    public static class PluginRules extends RuleSource {

        /**
         * Provide the Java configuration
         *
         * @param javaConfig Java configuration
         */
        @Model
        public void javaConfig(JavaConfig javaConfig) {
        }

        /**
         * Provide the Java plugin convention
         *
         * @param project Project to get convention from
         * @return Java plugin convention
         */
        @Model
        @Hidden
        public JavaPluginConvention javaPluginConvention(Project project) {
            return project.getConvention().getPlugin(JavaPluginConvention.class);
        }

        /**
         * Provide the base plugin convention
         *
         * @param project Project to get convention from
         * @return Base plugin convention
         */
        @Model
        @Hidden
        public BasePluginConvention basePluginConvention(Project project) {
            return project.getConvention().getPlugin(BasePluginConvention.class);
        }

        /**
         * Initialize the Java plugin convention
         *
         * @param javaConvention Java plugin convention to initialize
         */
        @Defaults
        public void initializeJavaPluginConvention(JavaPluginConvention javaConvention) {
            javaConvention.setSourceCompatibility("1.8");
            javaConvention.setTargetCompatibility("1.8");
        }

        /**
         * Configure the compile tasks
         *
         * @param javaCompile Compile task to configure
         */
        @Mutate
        public void configureCompileTask(@Each JavaCompile javaCompile) {
            List<String> compilerArgs = javaCompile.getOptions().getCompilerArgs();

            compilerArgs.add("-Xlint:unchecked");
            compilerArgs.add("-Xlint:deprecation");
        }

        /**
         * Create task to create sources JAR
         *
         * @param sourcesJar Sources JAR task to configure
         * @param source Source set to add to the JAR
         */
        @Mutate
        public void configureSourcesJarTask(@Path("tasks.sourcesJar") Jar sourcesJar, ProjectSourceSet source) {
            sourcesJar.setDescription("Assembles a jar archive containing the sources.");
            sourcesJar.setGroup("build");
            sourcesJar.setClassifier("sources");
            sourcesJar.from(source.withType(JavaSourceSet.class).stream().filter(s -> s.getParentName().equals("main"))
                    .findAny().get().getSource());
        }

        /**
         * Configure the javadoc task
         *
         * @param javadoc Javadoc task to configure
         */
        @Mutate
        public void configureJavadocTask(@Path("tasks.javadoc") Javadoc javadoc) {
            javadoc.setFailOnError(false);
        }

        /**
         * Create a task to create a JavaDoc JAR
         *
         * @param javadocJar Task to configure
         * @param javadoc JavaDoc task
         */
        @Mutate
        public void configureJavadocJarTask(@Path("tasks.javadocJar") Jar javadocJar,
                @Path("tasks.javadoc") Javadoc javadoc) {
            javadocJar.setDescription("Assembles a jar archive containing the JavaDoc documentation.");
            javadocJar.setGroup("build");
            javadocJar.setClassifier("javadoc");
            javadocJar.from(javadoc);
            javadocJar.dependsOn(javadoc);
        }

        /**
         * Configure task to create a JAR with the test classes
         *
         * @param testJar Task to configure
         * @param compileTestJava Java compile task for tests
         * @param processTestResources Java resources task for tests
         */
        @Mutate
        public void configureTestJarTask(@Path("tasks.testJar") Jar testJar,
                @Path("tasks.compileTestJava") Task compileTestJava,
                @Path("tasks.processTestResources") Task processTestResources) {
            testJar.setDescription("Assembles a jar archive containing the unit tests..");
            testJar.setGroup("build");
            testJar.from(compileTestJava, processTestResources);
            testJar.dependsOn(compileTestJava, processTestResources);
        }
    }

    /**
     * @see me.seeber.gradle.plugin.AbstractProjectConfigPlugin#initialize()
     */
    @Override
    protected void initialize() {
        getProject().getPluginManager().apply(JavaPlugin.class);
        getProject().getPluginManager().apply(ProjectConfigPlugin.class);

        getProject().getConfigurations().create(ANNOTATIONS_CONFIGURATION, c -> {
            c.setDescription("Eclipse external annotation archives");
            c.setVisible(false);
            c.setTransitive(true);
        });

        DependencyHandler dependencies = getProject().getDependencies();

        for (String configurationName : ImmutableList.of("compileOnly", "testCompileOnly")) {
            dependencies.add(configurationName, "org.eclipse.jdt:org.eclipse.jdt.annotation:2.0.0");
        }

        // Add published JARs to archives configuration
        String name = Validate.notNull(getProject().getName());
        Configuration archives = getProject().getConfigurations().getByName("archives");

        Jar sourcesJar = getProject().getTasks().create("sourcesJar", Jar.class);
        PublishArtifact sourcesArtifact = Projects.createJarPublishArtifact(getProject(), name, "sources", "jar", "jar",
                sourcesJar);
        archives.getArtifacts().add(sourcesArtifact);

        Jar javadocJar = getProject().getTasks().create("javadocJar", Jar.class);
        PublishArtifact javadocArtifact = Projects.createJarPublishArtifact(getProject(), name, "javadoc", "jar", "jar",
                javadocJar);
        archives.getArtifacts().add(javadocArtifact);

        // Create configuration for test archives
        String testName = name + "-test";
        Configuration testArchives = getProject().getConfigurations().create("testArchives");
        Configuration testRuntime = getProject().getConfigurations().getByName("testRuntime");

        Jar testJar = getProject().getTasks().create("testJar", Jar.class);
        testJar.setBaseName(testName);
        PublishArtifact testArtifact = Projects.createJarPublishArtifact(getProject(), testName, null, "jar", "jar",
                testJar);
        testArchives.getArtifacts().add(testArtifact);
        testRuntime.getArtifacts().add(testArtifact);
    }
}
