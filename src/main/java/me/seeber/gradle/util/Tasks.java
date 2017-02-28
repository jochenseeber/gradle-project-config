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

import org.gradle.api.Task;

/**
 * Utilities for {@link Task} objects
 */
public abstract class Tasks {

    /**
     * Case to start a name with
     */
    public enum AdjustCase {

        /**
         * No case adjustment
         */
        SAME {
            @Override
            public char convert(char ch) {
                return ch;
            }
        },

        /**
         * Start with lower case
         */
        LOWER {
            @Override
            public char convert(char ch) {
                return Character.toLowerCase(ch);
            }
        },

        /**
         * Start with upper case
         */
        UPPER {
            @Override
            public char convert(char ch) {
                return Character.toUpperCase(ch);
            }
        };

        /**
         * Convert a character to correct case
         *
         * @param ch Character
         * @return Case adjusted character
         */
        public abstract char convert(char ch);

    }

    /**
     * Get a task name
     *
     * @param prefix Prefix fo rthe task name
     * @param infix Infix for the task name
     * @param suffix Suffix for the task name
     *
     * @return Clean task name
     */
    public static final String taskName(String prefix, String infix, String suffix) {
        String name = namePart(prefix, AdjustCase.LOWER) + namePart(infix, AdjustCase.UPPER)
                + namePart(suffix, AdjustCase.UPPER);
        return name;
    }

    /**
     * Get the name of the clean task for a task name
     *
     * @param taskName Task name
     * @return Clean task name
     */
    public static final String cleanName(String taskName) {
        return "clean" + namePart(taskName, AdjustCase.UPPER);
    }

    /**
     * Get a task name part for a name
     *
     * @param name Name
     * @param adjustCase Case adjustment
     * @return Name part
     */
    public static String namePart(String name, AdjustCase adjustCase) {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < name.length(); ++i) {
            char ch = name.charAt(i);

            switch (ch) {
                case '-':
                case '_':
                case ' ': {
                    adjustCase = AdjustCase.UPPER;
                    break;
                }

                default: {
                    result.append(adjustCase.convert(ch));
                    adjustCase = AdjustCase.SAME;
                    break;
                }
            }
        }

        return result.toString();
    }

}
