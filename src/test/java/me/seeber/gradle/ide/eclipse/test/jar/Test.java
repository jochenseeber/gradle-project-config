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
package me.seeber.gradle.ide.eclipse.test.jar;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;

@SuppressWarnings("javadoc")
public abstract class Test {

    public abstract void method();

    public abstract int methodWithIntParameter(int parameter);

    public abstract Object methodWithParameter(Object parameter);

    public abstract String[] methodWithArrayParameter(String[] parameter);

    public abstract List<String> methodWithGenericParameter(List<String> parameter);

    public abstract List<?> methodWithWildcardParameter(List<?> parameter);

    public abstract List<? extends Serializable> methodWithUpperBoundedParameter(
            List<? extends Serializable> parameter);

    public abstract Comparator<? super String> methodWithLowerBoundedParameter(Comparator<? super String> parameter);

    public abstract <O> O genericMethod(O object);

    public abstract <A> A[] genericArrayMethod(A[] array);

    public abstract <N extends Number> N genericMethodWithUpperClassBound(N number);

    public abstract <S extends Serializable> S genericMethodWithUpperInterfaceBound(S parameter);

    public abstract <S extends Number & Serializable> S genericMethodWithUpperClassAndInterfaceBound(S parameter);

    public abstract <S extends Serializable & Appendable> S genericMethodWithTwoUpperInterfaceBounds(S parameter);

}
