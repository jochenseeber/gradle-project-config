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

package me.seeber.gradle.plugin;

import static java.lang.String.format;

import org.eclipse.jdt.annotation.Nullable;
import org.gradle.BuildAdapter;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.util.VersionNumber;

import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;

import me.seeber.gradle.util.Validate;

/**
 * Base class for project configuration plugins
 */
public abstract class AbstractProjectConfigPlugin implements Plugin<Project> {

    /**
     * Minimum required Gradle version
     */
    protected static final VersionNumber MIN_GRADLE_VERSION = VersionNumber.parse("3.0");

    /**
     * Formatter used to create the extension name from the extension class
     */
    protected static final Converter<String, String> NAME_FORMATTER = Validate
            .notNull(CaseFormat.UPPER_CAMEL.converterTo(CaseFormat.LOWER_CAMEL));

    /**
     * Project the plugin was applied to
     */
    private @Nullable Project project;

    /**
     * Logger in case you have something to say...
     */
    private final Logger logger;

    /**
     * Create a new plugin
     */
    public AbstractProjectConfigPlugin() {
        this.logger = Logging.getLogger(getClass());
    }

    /**
     * @see org.gradle.api.Plugin#apply(java.lang.Object)
     */
    @Override
    public void apply(Project project) {
        this.project = project;

        if (VersionNumber.parse(project.getGradle().getGradleVersion()).compareTo(MIN_GRADLE_VERSION) < 0) {
            throw new GradleException(format("Base project plugin requires Gradle %s or higher", MIN_GRADLE_VERSION));
        }

        getLogger().info("Applying plugin {} to {}", this, getProject());

        initialize();

        getProject().afterEvaluate(p -> configure());

        getProject().getGradle().addBuildListener(new BuildAdapter() {
            @Override
            public void projectsEvaluated(@Nullable Gradle gradle) {
                complete();
            }
        });
    }

    /**
     * Initialize the plugin
     * 
     * Called when the plugin is applied. Subclasses may override to implement custom functionality
     */
    protected void initialize() {
    }

    /**
     * Configure the plugin
     * 
     * Called when the project has been evaluated. Subclasses may override to implement custom functionality
     */
    protected void configure() {
    }

    /**
     * Complete the plugin setup
     * 
     * Called when all projects have been evaluated. Subclasses may override to implement custom functionality
     */
    protected void complete() {
    }

    /**
     * Get the logger
     * 
     * @return Logger if you want to be chatty
     */
    protected Logger getLogger() {
        return this.logger;
    }

    /**
     * Get the project the plugin was applied to
     * 
     * @return Gradle project
     */
    public Project getProject() {
        Project project = this.project;

        if (project == null) {
            throw new IllegalStateException("Project is only accessible after plugin has been applied");
        }

        return project;
    }

}
