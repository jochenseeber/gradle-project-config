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

package me.seeber.gradle.util;

import static java.lang.String.format;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Optional;
import java.util.Properties;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

/**
 * Helpers for class {@link Class}
 */
public class Classes {

    /**
     * Get a resource as string
     *
     * @param type Context class
     * @param name Name of the resource
     * @return Resource content
     */
    public static Optional<String> getResourceString(Class<?> type, String name) {
        String text = null;
        URL resource = type.getResource(name);

        if (resource != null) {
            try {
                text = Resources.toString(resource, Charsets.UTF_8);
            }
            catch (IOException e) {
                throw new RuntimeException(String.format("Could not read resource '%s'", resource));
            }
        }

        return Optional.ofNullable(text);
    }

    /**
     * Load properties from a resource
     *
     * @param type Type to load properties for
     * @param name Properties name
     * @return Properties
     */
    public static Properties loadProperties(Class<?> type, String name) {
        try {
            URL url = Resources.getResource(type, name);

            try (InputStream in = url.openStream()) {
                Properties properties = new Properties();
                properties.load(in);
                return properties;
            }
        }
        catch (Exception e) {
            throw new RuntimeException(format("Could not load properties '%s' for type '%s'", name, type), e);
        }
    }

}
