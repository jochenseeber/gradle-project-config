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

import java.io.File;

import org.eclipse.jdt.annotation.Nullable;
import org.gradle.api.Project;
import org.gradle.api.plugins.BasePluginConvention;

import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;

import me.seeber.gradle.util.Validate;

/**
 * Information about the current project
 */
public class ProjectContext {

    /**
     * Formatter used to convert property names to environment names
     */
    protected static final Converter<String, String> PROPERTY_NAME_CONVERTER = Validate
            .notNull(CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.UPPER_UNDERSCORE));

    /**
     * Project to provide the context for
     */
    private final Project project;

    /**
     * Create a new project context
     *
     * @param project Project to provide the context for
     */
    public ProjectContext(Project project) {
        this.project = project;
    }

    /**
     * Get a property from the project or the environment
     *
     * First looks up the property value in the project's properties. If the property is not set, converts the property
     * name to upper underscore format and checks if the value is set in the environment.
     *
     * @param name Property name
     * @return Property value or <code>null</code> if the property does not exist
     */
    public @Nullable String getProperty(String name) {
        String env = toEnvName(name);
        String property = name.toLowerCase();
        String value = System.getProperty(env);

        if (value == null) {
            value = (String) getProject().findProperty(property);
        }

        return value;
    }

    /**
     * Get the project name
     *
     * @return Project name
     */
    public String getName() {
        return Validate.notNull(getProject().getName());
    }

    /**
     * Get the project group
     *
     * @return Project group
     */
    public String getGroup() {
        return Validate.notNull(this.project.getGroup().toString());
    }

    /**
     * Get the project version
     *
     * @return Project version
     */
    public String getVersion() {
        return Validate.notNull(getProject().getVersion().toString());
    }

    /**
     * Get the project description
     *
     * @return Project description
     */
    public @Nullable String getDescription() {
        return getProject().getDescription();
    }

    /**
     * Get the library directory
     *
     * @return Library directory
     */
    public File getLibsDir() {
        BasePluginConvention baseConvention = this.project.getConvention().getPlugin(BasePluginConvention.class);
        File libsDir = Validate.notNull(baseConvention.getLibsDir(), "The libs dir must not be null");
        return libsDir;
    }

    /**
     * Get the project directory
     *
     * @return Project directory
     */
    public File getProjectDir() {
        return Validate.notNull(getProject().getProjectDir());
    }

    /**
     * Get the context project
     *
     * @return Project
     */
    protected Project getProject() {
        return this.project;
    }

    /**
     * Get the environment name for a property
     *
     *
     *
     * @param name Property name
     * @return Environment name for property
     */
    protected String toEnvName(String name) {
        return Validate.notNull(PROPERTY_NAME_CONVERTER.convert(name));
    }

}
