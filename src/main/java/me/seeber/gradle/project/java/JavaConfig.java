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

import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.gradle.model.Managed;
import org.gradle.model.Unmanaged;

/**
 * Java configuration
 */
@Managed
public interface JavaConfig {

    /**
     * Get the SLF4J version
     *
     * @return SLF4J version
     */
    public @Nullable String getSlf4jVersion();

    /**
     * Set the SLF4J version
     *
     * @param version SLF4J version
     */
    public void setSlf4jVersion(@Nullable String version);

    /**
     * Get the JaCoCo version
     *
     * @return JaCoCo version
     */
    public @Nullable String getJacocoVersion();

    /**
     * Set the JaCoCo version
     *
     * @param version JaCoCo version
     */
    public void setJacocoVersion(@Nullable String version);

    /**
     * Get the eclipse compiler version
     *
     * @return Eclipse compiler version
     */
    public @Nullable String getEclipseCompilerVersion();

    /**
     * Set the eclipse compiler version
     *
     * @param version Eclipse compiler version
     */
    public void setEclipseCompilerVersion(@Nullable String version);

    /**
     * Get Eclipse Java compiler properties
     *
     * @return Eclipse Java compiler properties
     */
    @Unmanaged
    public Map<String, String> getEclipseCompilerProperties();

    /**
     * Set Eclipse Java compiler properties
     *
     * @param properties Eclipse Java compiler properties
     */
    public void setEclipseCompilerProperties(Map<String, String> properties);

    /**
     * Get if the compiler should optimize
     *
     * @return <code>true</code> if the compiler should optimize
     */
    public @Nullable Boolean isOptimize();

    /**
     * Set if the compiler should optimize
     *
     * @param optimize <code>true</code> if the compiler should optimize
     */
    public void setOptimize(@Nullable Boolean optimize);

    /**
     * Get if the compiler should generate debug information
     *
     * @return <code>true</code> if the compiler should generate debug information
     */
    public @Nullable Boolean isDebug();

    /**
     * Set if the compiler should generate debug information
     *
     * @param debug <code>true</code> if the compiler should generate debug information
     */
    public void setDebug(@Nullable Boolean debug);
}
