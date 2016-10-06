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

package me.seeber.gradle.util;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Validation utilities
 */
public abstract class Validate {

    /**
     * Validate that an argument is not <code>null</code>
     *
     * @param argument Argument to validate
     * @param <T> Type of argument
     * @return Argument
     * @throws NullPointerException if argument is <code>null</code>
     */
    public static <T> @NonNull T notNull(@Nullable T argument) throws NullPointerException {
        if (argument == null) {
            throw new NullPointerException();
        }

        return argument;
    }

    /**
     * Validate that an argument is not <code>null</code>
     *
     * @param argument Argument to validate
     * @param message Exception message
     * @param parameters Exception message parameters
     * @param <T> Type of argument
     * @return Argument
     * @throws NullPointerException if argument is <code>null</code>
     */
    public static <T> @NonNull T notNull(@Nullable T argument, String message, Object... parameters)
            throws NullPointerException {
        if (argument == null) {
            throw new NullPointerException(String.format(message, parameters));
        }

        return argument;
    }
}
