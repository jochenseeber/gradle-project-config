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
package me.seeber.gradle.distribution.maven;

import java.util.List;

import org.gradle.api.Named;
import org.gradle.model.Managed;

/**
 * Maven publication configuration
 */
@Managed
public interface MavenPublicationConfig extends Named {

    /**
     * Get the artifact ID of the publication
     *
     * @return Artifact ID
     */
    public String getArtifactId();

    /**
     * Set the artifact ID of the publication
     *
     * @param id Artifact ID
     */
    public void setArtifactId(String id);

    /**
     * Get the name of the configuration containing the archives to publish
     *
     * @return Configuration with archives to publish
     */
    public String getArchivesConfiguration();

    /**
     * Set the name of the configuration containing the archives to publish
     *
     * @param configuration Configuration with archives to publish
     */
    public void setArchivesConfiguration(String configuration);

    /**
     * Get if the artifacts generated by the project should be added
     *
     * @return <code>true</code> if the artifacts generated by the project should be added
     */
    public boolean isAddProjectArtifacts();

    /**
     * Set if the artifacts generated by the project should be added
     *
     * @param add <code>true</code> if the artifacts generated by the project should be added
     */
    public void setAddProjectArtifacts(boolean add);

    /**
     * Get the compile configurations used for the publication
     *
     * @return Compile configurations
     */
    public List<String> getCompileConfigurations();

    /**
     * Set the compile configurations used for the publication
     *
     * @param configurations Compile configurations
     */
    public void setCompileConfigurations(List<String> configurations);

    /**
     * Get the runtime configurations used for the publication
     *
     * @return Runtime configurations
     */
    public List<String> getRuntimeConfigurations();

    /**
     * Set the runtime configurations used for the publication
     *
     * @param configurations Runtime configurations
     */
    public void setRuntimeConfigurations(List<String> configurations);

}
