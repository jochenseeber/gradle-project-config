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
import java.io.IOException;

import org.eclipse.jdt.annotation.Nullable;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

/**
 * Update checkstyle configuration
 */
public class CheckstyleConfigUpdate extends ConventionTask {

    /**
     * Checkstyle configuration
     */
    @Input
    private @Nullable String config;

    /**
     * Checkstyle configuration file to write
     */
    @OutputFile
    private @Nullable File configFile;

    /**
     * Write the checkstyle configuration
     * 
     * @throws IOException if something goes wrong
     */
    @TaskAction
    public void updateConfig() throws IOException {
        Files.write(this.config, this.configFile, Charsets.UTF_8);
    }

    /**
     * Get the checkstyle configuration file to write
     * 
     * @return Checkstyle configuration file to write
     */
    public @Nullable File getConfigFile() {
        return this.configFile;
    }

    /**
     * Set the Checkstyle configuration file to write
     * 
     * @param configFile Checkstyle configuration file to write
     */
    public void setConfigFile(@Nullable File configFile) {
        this.configFile = configFile;
    }

    /**
     * Get the Checkstyle configuration
     * 
     * @return Checkstyle configuration
     */
    public @Nullable String getConfig() {
        return this.config;
    }

    /**
     * Set the Checkstyle configuration
     * 
     * @param config Checkstyle configuration
     */
    public void setConfig(@Nullable String config) {
        this.config = config;
    }

}
