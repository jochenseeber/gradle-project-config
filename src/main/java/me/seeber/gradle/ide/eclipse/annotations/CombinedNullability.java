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

package me.seeber.gradle.ide.eclipse.annotations;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.gradle.internal.impldep.com.google.common.collect.Iterables;

import com.google.common.collect.ImmutableList;

import me.seeber.gradle.ide.eclipse.annotations.MethodSignatureWriter.Nullness;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;

/**
 * Nullability that combines several nullabilities
 */
public class CombinedNullability implements Nullability {

    /**
     * Nullabilities to combine
     */
    private List<@NonNull Nullability> nullabilities;

    /**
     * Create a new provider
     *
     * @param nullabilities Nullabilities to combine
     */
    public CombinedNullability(@NonNull Nullability... nullabilities) {
        this.nullabilities = Objects.requireNonNull(ImmutableList.copyOf(nullabilities));
    }

    /**
     * Create a new provider
     *
     * @param nullabilities Nullabilities to combine
     */
    public CombinedNullability(List<@NonNull Nullability> nullabilities) {
        this.nullabilities = Objects.requireNonNull(ImmutableList.copyOf(nullabilities));
    }

    /**
     * @see me.seeber.gradle.ide.eclipse.annotations.Nullability#getParameterNullability(net.bytebuddy.description.method.ParameterDescription)
     */
    @Override
    public Nullness getParameterNullability(ParameterDescription parameter) {
        Nullness nullability = Nullness.UNDEFINED;

        for (Nullability provider : this.nullabilities) {
            Nullness override = provider.getParameterNullability(parameter);
            nullability = nullability.override(override);
        }

        return nullability;
    }

    /**
     * @see me.seeber.gradle.ide.eclipse.annotations.Nullability#getReturnValueNullability(MethodDescription)
     */
    @Override
    public Nullness getReturnValueNullability(MethodDescription method) {
        Nullness nullability = Nullness.UNDEFINED;

        for (Nullability provider : this.nullabilities) {
            Nullness override = provider.getReturnValueNullability(method);
            nullability = nullability.override(override);
        }

        return nullability;
    }

    /**
     * @see me.seeber.gradle.ide.eclipse.annotations.Nullability#override(me.seeber.gradle.ide.eclipse.annotations.Nullability)
     */
    @Override
    public Nullability override(Nullability override) {
        List<@NonNull Nullability> providers = Objects.requireNonNull(
                ImmutableList.copyOf(Iterables.concat(this.nullabilities, Collections.singleton(override))));
        CombinedNullability result = new CombinedNullability(providers);
        return result;
    }

}
