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

package me.seeber.gradle.project.base;

import org.eclipse.jdt.annotation.Nullable;
import org.gradle.api.Named;
import org.gradle.model.Managed;

/**
 * Repository configuration
 */
@Managed
public interface Repository extends Named {

    /**
     * Get the ID of the repository
     *
     * @return ID of the repository
     */
    public @Nullable String getId();

    /**
     * Set the ID of the repository
     *
     * @param id ID of the repository
     */
    public void setId(@Nullable String id);

    /**
     * Get the type of the repository used in the repository URL
     *
     * Used together with the (developer) connection to create URIs for the repository. Examples are "git" or "svn".
     *
     * @return Type of the repository used used in the repository URL
     */
    public @Nullable String getType();

    /**
     * Set the type of the repository used in the repository URL
     *
     * @param type Type of the repository used used in the repository URL
     */
    public void setType(@Nullable String type);

    /**
     * Get the URL of the repository's web site
     *
     * @return URL of the repository's web site
     */
    public @Nullable String getWebsiteUrl();

    /**
     * Set the URL of the repository's web site
     *
     * @param url URL of the repository's web site
     */
    public void setWebsiteUrl(@Nullable String url);

    /**
     * Get the connection for the repository
     *
     * @return Connection for the repository
     */
    public @Nullable String getConnection();

    /**
     * Set the connection for the repository
     *
     * @param connection Connection for the repository
     */
    public void setConnection(@Nullable String connection);

    /**
     * Get the developer connection for the repository
     *
     * @return Developer connection for the repository
     */
    public @Nullable String getDeveloperConnection();

    /**
     * Set the developer connection for the repository
     *
     * @param connection Developer connection for the repository
     */
    public void setDeveloperConnection(@Nullable String connection);

}
