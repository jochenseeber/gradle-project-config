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
package me.seeber.gradle.project.annotations;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gradle.api.GradleException;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.model.Defaults;
import org.gradle.model.Model;
import org.gradle.model.RuleSource;
import org.gradle.model.Validate;

import com.google.common.base.Strings;

import me.seeber.gradle.plugin.AbstractProjectConfigPlugin;
import me.seeber.gradle.project.base.License;
import me.seeber.gradle.project.base.ProjectConfig;
import me.seeber.gradle.project.base.ProjectConfigPlugin;
import me.seeber.gradle.project.base.ProjectContext;

/**
 * Plugin to configure a project containing external Eclipse annotations
 */
public class AnnotationsConfigPlugin extends AbstractProjectConfigPlugin {

    /**
     * Pattern to extract the ID of the annotated project from the build name
     */
    protected static final Pattern ID_PATTERN = Pattern.compile("^(.+)-annotations$", Pattern.CASE_INSENSITIVE);

    /**
     * Pattern to extract the version of the annotated project from the build version
     */
    protected static final Pattern VERSION_PATTERN = Pattern.compile("^([^-]+)(?:-.+)?$", Pattern.CASE_INSENSITIVE);

    /**
     * Plugin rules
     */
    public static class PluginRules extends RuleSource {

        /**
         * Provide the annotations configuration
         *
         * @param annotatedProject Annotation configuration
         * @param extensions Extension container
         */
        @Model
        public void annotatedProject(AnnotatedProject annotatedProject, ExtensionContainer extensions) {
            extensions.getExtraProperties().set("annotatedProject", annotatedProject);
        }

        /**
         * Initialize the annotations configuration
         *
         * @param annotatedProject Annotations configuration to initialize
         * @param project Project context
         */
        @Defaults
        public void initializeAnnotationsConfig(AnnotatedProject annotatedProject, ProjectContext project) {
            Matcher matcher = ID_PATTERN.matcher(project.getName());

            if (matcher.matches()) {
                annotatedProject.setId(matcher.group(1));
            }

            matcher = VERSION_PATTERN.matcher(project.getVersion().toString());

            if (matcher.matches()) {
                annotatedProject.setVersion(matcher.group(1));
            }
        }

        /**
         * Validate the annotations configuration
         *
         * @param annotatedProject Annotations configuration to initialize
         */
        @Validate
        public void validateAnnotationsConfig(AnnotatedProject annotatedProject) {
            if (Strings.isNullOrEmpty(annotatedProject.getId())) {
                throw new GradleException("Please set annotatedProject.id");
            }

            if (Strings.isNullOrEmpty(annotatedProject.getVersion())) {
                throw new GradleException("Please set annotatedProject.version");
            }

            if (Strings.isNullOrEmpty(annotatedProject.getName())) {
                throw new GradleException("Please set annotatedProject.name");
            }

            if (Strings.isNullOrEmpty(annotatedProject.getUrl())) {
                throw new GradleException("Please set annotationsConfig.url");
            }
        }

        /**
         * Initialize the project configuration
         *
         * @param projectConfig Project configuration
         * @param annotationsConfig Annotations configuration
         */
        @Defaults
        public void initializeProjectConfig(ProjectConfig projectConfig, AnnotatedProject annotationsConfig) {
            License license = projectConfig.getLicense();

            license.exclude("*.eea");
        }
    }

    /**
     * @see me.seeber.gradle.plugin.AbstractProjectConfigPlugin#initialize()
     */
    @Override
    protected void initialize() {
        getProject().getPlugins().apply(ProjectConfigPlugin.class);
        getProject().getPlugins().apply(JavaPlugin.class);

        getProject().getConfigurations().create("annotate", c -> {
            c.setDescription("Modules annotated by this project.");
            c.setVisible(true);
            c.setTransitive(false);
        });
    }
}
