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

package me.seeber.gradle.ide.eclipse.test.jar;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;

@SuppressWarnings("javadoc")
public abstract class NullableGenericTest<T> implements Comparator<T> {

    public abstract class NestedGenericTest<U> {

        public abstract @MaybeNil U methodWithVariableParameter(@MaybeNil T parameter, @MaybeNil U parameter2);

    }

    public abstract void method();

    public abstract int methodWithIntParameter(int parameter);

    public abstract @MaybeNil Object methodWithParameter(@MaybeNil Object parameter);

    public abstract @MaybeNil String[] methodWithArrayParameter(@MaybeNil String[] parameter);

    public abstract @MaybeNil T methodWithVariableParameter(@MaybeNil T parameter);

    public abstract @MaybeNil List<String> methodWithGenericParameter(@MaybeNil List<String> parameter);

    public abstract @MaybeNil List<?> methodWithWildcardParameter(@MaybeNil List<?> parameter);

    public abstract @MaybeNil List<? extends Serializable> methodWithUpperBoundedParameter(
            @MaybeNil List<? extends Serializable> parameter);

    public abstract @MaybeNil Comparator<? super String> methodWithLowerBoundedParameter(
            @MaybeNil Comparator<? super String> parameter);

    public abstract @MaybeNil <O> O genericMethod(@MaybeNil O object);

    public abstract @MaybeNil <A> A[] genericArrayMethod(@MaybeNil A[] array);

    public abstract @MaybeNil <N extends Number> N genericMethodWithUpperClassBound(@MaybeNil N number);

    public abstract @MaybeNil <S extends Serializable> S genericMethodWithUpperInterfaceBound(@MaybeNil S parameter);

    public abstract @MaybeNil <S extends Number & Serializable> S genericMethodWithUpperClassAndInterfaceBound(
            @MaybeNil S parameter);

    public abstract @MaybeNil <S extends Serializable & Appendable> S genericMethodWithTwoUpperInterfaceBounds(
            @MaybeNil S parameter);

    public abstract @MaybeNil <U> NestedGenericTest<U> methodWithNestedGenericParameter(
            @MaybeNil NestedGenericTest<U> parameter);

}
