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

package me.seeber.gradle.project.annotations;

import org.eclipse.jdt.annotation.Nullable;
import org.gradle.model.Managed;

/**
 * Annotations configuration
 */
@Managed
public interface AnnotatedProject {

    /**
     * Get the version of the annotated project
     *
     * @return Version of the annotated project
     */
    public @Nullable String getId();

    /**
     * Set the version of the annotated project
     *
     * @param id Version of the annotated project
     */
    public void setId(@Nullable String id);

    /**
     * Get the version of the annotated project
     *
     * @return Version of the annotated project
     */
    public @Nullable String getVersion();

    /**
     * Set the version of the annotated project
     *
     * @param version Version of the annotated project
     */
    public void setVersion(@Nullable String version);

    /**
     * Get the name of the annotated project
     *
     * @return Name of the annotated project
     */
    public @Nullable String getName();

    /**
     * Set the name of the annotated project
     *
     * @param description Name of the annotated project
     */
    public void setName(@Nullable String description);

    /**
     * Get the name of the annotated project
     *
     * @return Name of the annotated project
     */
    public @Nullable String getUrl();

    /**
     * Set the name of the annotated project
     *
     * @param description Name of the annotated project
     */
    public void setUrl(@Nullable String description);

}
