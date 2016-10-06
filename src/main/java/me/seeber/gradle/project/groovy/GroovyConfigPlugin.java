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

package me.seeber.gradle.project.groovy;

import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ExternalDependency;
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.plugins.GroovyPlugin;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.javadoc.Groovydoc;
import org.gradle.model.Model;
import org.gradle.model.Mutate;
import org.gradle.model.Path;
import org.gradle.model.RuleSource;

import com.google.common.collect.ImmutableMap;

import me.seeber.gradle.plugin.AbstractProjectConfigPlugin;
import me.seeber.gradle.plugin.Projects;
import me.seeber.gradle.project.java.JavaConfigPlugin;
import me.seeber.gradle.util.Validate;

/**
 * Groovy project configuration
 */
public class GroovyConfigPlugin extends AbstractProjectConfigPlugin {

    /**
     * Plugin rules
     */
    public static class PluginRules extends RuleSource {

        /**
         * Provide the Groovy configuration
         *
         * @param groovyConfig Groovy configuration
         */
        @Model
        public void groovyConfig(GroovyConfig groovyConfig) {
        }

        /**
         * Create task to create the GroovyDoc jar
         *
         * @param task Tasl to configure
         * @param groovydocTask Groovydoc task
         */
        @Mutate
        public void configureGroovydocJarTask(@Path("tasks.groovydocJar") Jar task,
                @Path("tasks.groovydoc") Groovydoc groovydocTask) {
            task.setDescription("Assembles a jar archive containing the groovydoc documentation.");
            task.setGroup("build");
            task.setClassifier("groovydoc");
            task.from(groovydocTask);
        }
    }

    /**
     * @see me.seeber.gradle.plugin.AbstractProjectConfigPlugin#initialize()
     */
    @Override
    protected void initialize() {
        getProject().getPluginManager().apply(GroovyPlugin.class);
        getProject().getPluginManager().apply(JavaConfigPlugin.class);

        DependencyHandler dependencies = getProject().getDependencies();
        ExternalDependency spock = (ExternalDependency) dependencies.add("testCompile",
                ImmutableMap.of("group", "org.spockframework", "name", "spock-core", "version", "1.0-groovy-2.4"));
        spock.exclude(ImmutableMap.of("group", "org.codehaus.groovy"));

        String name = Validate.notNull(getProject().getName());
        Configuration archives = getProject().getConfigurations().getByName("archives");

        Jar groovydocJar = getProject().getTasks().create("groovydocJar", Jar.class);
        PublishArtifact groovydocArtifact = Projects.createJarPublishArtifact(getProject(), name, "groovydoc", "jar",
                "jar", groovydocJar);
        archives.getArtifacts().add(groovydocArtifact);
    }
}
