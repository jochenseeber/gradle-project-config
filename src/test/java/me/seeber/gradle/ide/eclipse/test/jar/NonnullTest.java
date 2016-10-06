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
public abstract class NonnullTest {

    public abstract void method();

    public abstract int methodWithIntParameter(int parameter);

    public abstract @NeverNil Object methodWithParameter(@NeverNil Object parameter);

    public abstract @NeverNil String[] methodWithArrayParameter(@NeverNil String[] parameter);

    public abstract @NeverNil List<String> methodWithGenericParameter(@NeverNil List<String> parameter);

    public abstract @NeverNil List<?> methodWithWildcardParameter(@NeverNil List<?> parameter);

    public abstract @NeverNil List<? extends Serializable> methodWithUpperBoundedParameter(
            @NeverNil List<? extends Serializable> parameter);

    public abstract @NeverNil Comparator<? super String> methodWithLowerBoundedParameter(
            @NeverNil Comparator<? super String> parameter);

    public abstract @NeverNil <O> O genericMethod(@NeverNil O object);

    public abstract @NeverNil <A> A[] genericArrayMethod(@NeverNil A[] array);

    public abstract @NeverNil <N extends Number> N genericMethodWithUpperClassBound(@NeverNil N number);

    public abstract @NeverNil <S extends Serializable> S genericMethodWithUpperInterfaceBound(@NeverNil S parameter);

    public abstract @NeverNil <S extends Number & Serializable> S genericMethodWithUpperClassAndInterfaceBound(
            @NeverNil S parameter);

    public abstract @NeverNil <S extends Serializable & Appendable> S genericMethodWithTwoUpperInterfaceBounds(
            @NeverNil S parameter);

}
