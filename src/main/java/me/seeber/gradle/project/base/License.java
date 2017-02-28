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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.Nullable;
import org.gradle.model.Managed;

/**
 * License configuration
 */
@Managed
public abstract class License {

    /**
     * Get the ID of the license
     *
     * @return ID of the license
     */
    public abstract @Nullable String getId();

    /**
     * Set the ID of the license
     *
     * @param id ID of the license
     */
    public abstract void setId(@Nullable String id);

    /**
     * Get the URL where the license can be viewed
     *
     * @return URL where the license can be viewed
     */
    public abstract @Nullable String getUrl();

    /**
     * Set the URL where the license can be viewed
     *
     * @param url URL where the license can be viewed
     */
    public abstract void setUrl(@Nullable String url);

    /**
     * Get the URL where the license can be downloaded
     *
     * @return URL where the license can be downloaded
     */
    public abstract @Nullable String getSourceUrl();

    /**
     * Set the URL where the license can be downloaded
     *
     * @param url URL where the license can be downloaded
     */
    public abstract void setSourceUrl(@Nullable String url);

    /**
     * Get exclude patterns for license checking
     *
     * @return Exclude patterns
     */
    public abstract @Nullable List<String> getExcludes();

    /**
     * Set exclude patterns for license checking
     *
     * @param excludes Exclude patterns
     */
    public abstract void setExcludes(@Nullable List<String> excludes);

    /**
     * Add an exclude pattern for license checking
     *
     * @param exclude Exclude pattern to add
     */
    public void exclude(String exclude) {
        List<String> excludes = (getExcludes() == null) ? new ArrayList<>() : new ArrayList<>(getExcludes());
        excludes.add(exclude);
        setExcludes(excludes);
    }

}
