/**
 * BSD 2-Clause License
 *
 * Copyright (c) 2016, Jochen Seeber
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

package me.seeber.gradle.project.gradle;

import org.gradle.api.Task;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.model.Defaults;
import org.gradle.model.Each;
import org.gradle.model.Mutate;
import org.gradle.model.RuleSource;
import org.gradle.plugin.devel.GradlePluginDevelopmentExtension;
import org.gradle.plugin.devel.plugins.JavaGradlePluginPlugin;

import me.seeber.gradle.plugin.AbstractProjectConfigPlugin;
import me.seeber.gradle.project.base.ProjectConfigPlugin;
import me.seeber.gradle.project.groovy.GroovyConfigPlugin;
import me.seeber.gradle.project.java.JavaConfigPlugin;

/**
 * Gradle plugin project configuration
 */
public class GradlePluginConfigPlugin extends AbstractProjectConfigPlugin {

    /**
     * Plugin rules
     */
    public static class PluginRules extends RuleSource {

        /**
         * Initialize the Gradle plugin extension
         *
         * @param pluginExtension Gradle plugin extension to initialize
         */
        @Defaults
        public void initializeGradlePluginDevelopmentExtension(GradlePluginDevelopmentExtension pluginExtension) {
            pluginExtension.setAutomatedPublishing(false);
        }

        /**
         * Configure the eclipse task
         *
         * @param task Task to configure
         * @param pluginExtension Plugin extension
         */
        @Mutate
        public void configureTasks(@Each Task task, GradlePluginDevelopmentExtension pluginExtension) {
            if (task.getName().equals("eclipse")) {
                task.dependsOn("pluginUnderTestMetadata");
            }
        }

    }

    /**
     * @see me.seeber.gradle.plugin.AbstractProjectConfigPlugin#initialize()
     */
    @Override
    public void initialize() {
        getProject().getPluginManager().apply(ProjectConfigPlugin.class);
        getProject().getPluginManager().apply(JavaConfigPlugin.class);
        getProject().getPluginManager().apply(GroovyConfigPlugin.class);
        getProject().getPluginManager().apply(JavaGradlePluginPlugin.class);

        DependencyHandler dependencies = getProject().getDependencies();
        dependencies.add("compile", dependencies.gradleApi());
        dependencies.add("compile", dependencies.localGroovy());

        dependencies.add("testCompile", dependencies.gradleTestKit());
        dependencies.add("testCompile", "me.seeber.gradle:gradle-test-kit:1.0.0-SNAPSHOT");
    }
}
