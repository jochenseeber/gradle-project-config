/*
 * BSD 2-Clause License
 *
 * Copyright (c) 2016, Jochen Seeber
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
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
package me.seeber.gradle.ide.eclipse.annotations.test

import groovy.transform.TypeChecked
import me.seeber.gradle.ide.eclipse.annotations.AnnotationNullability
import me.seeber.gradle.ide.eclipse.annotations.ConstantNullability
import me.seeber.gradle.ide.eclipse.annotations.MethodSignatureWriter
import me.seeber.gradle.ide.eclipse.annotations.Nullability
import me.seeber.gradle.ide.eclipse.annotations.MethodSignatureWriter.Nullness
import me.seeber.gradle.ide.eclipse.test.jar.GenericTest
import me.seeber.gradle.ide.eclipse.test.jar.MaybeNil
import me.seeber.gradle.ide.eclipse.test.jar.NeverNil
import me.seeber.gradle.ide.eclipse.test.jar.NonnullTest
import me.seeber.gradle.ide.eclipse.test.jar.NullableTest
import me.seeber.gradle.ide.eclipse.test.jar.ParametersAreNeverNilByDefault
import me.seeber.gradle.ide.eclipse.test.jar.Test
import me.seeber.gradle.ide.eclipse.test.jar.GenericTest.NestedGenericTest
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.pool.TypePool
import spock.lang.Specification

@TypeChecked
@SuppressWarnings("javadoc")
class MethodSignatureWriterTest extends Specification {

    static final Nullability NULLABLE_PARAMETERS = new ConstantNullability(Nullness.NULLABLE, Nullness.UNDEFINED)

    static final Nullability NONNULL_PARAMETERS = new ConstantNullability(Nullness.NONNULL, Nullness.UNDEFINED)

    static final Nullability ANNOTATED = new AnnotationNullability(MaybeNil.name, NeverNil.name, ParametersAreNeverNilByDefault.name)

    static final String GENERIC_TEST_CLASS_NAME = GenericTest.name.replaceAll("\\.", "/")

    def Map<String, String> createMethodSignatures(Class<?> clazz, Nullability nullabilityProvider) {
        TypePool types = TypePool.ClassLoading.of(clazz.classLoader)
        TypeDescription type = types.describe(clazz.name).resolve()
        MethodSignatureWriter writer = new MethodSignatureWriter(nullabilityProvider)

        type.declaredMethods.collectEntries { MethodDescription m ->
            [
                m.name,
                writer.appendMethodSignature(m, new StringBuilder()).toString()
            ]
        }
    }

    def "creates_method_signatures"(String method, String signature) {
        setup:
        Map<String, String> signatures = createMethodSignatures(Test, Nullability.omit())

        expect:
        signatures.getAt(method) == signature

        where:
        method || signature
        "method" || "()V"
        "methodWithIntParameter" || "(I)I"
        "methodWithParameter" || "(Ljava/lang/Object;)Ljava/lang/Object;"
        "methodWithArrayParameter" || "([Ljava/lang/String;)[Ljava/lang/String;"
        "methodWithGenericParameter" || "(Ljava/util/List<Ljava/lang/String;>;)Ljava/util/List<Ljava/lang/String;>;"
        "methodWithWildcardParameter" || "(Ljava/util/List<*>;)Ljava/util/List<*>;"
        "methodWithUpperBoundedParameter" || "(Ljava/util/List<+Ljava/io/Serializable;>;)Ljava/util/List<+Ljava/io/Serializable;>;"
        "methodWithLowerBoundedParameter" || "(Ljava/util/Comparator<-Ljava/lang/String;>;)Ljava/util/Comparator<-Ljava/lang/String;>;"
        "genericMethod" || "<O:Ljava/lang/Object;>(TO;)TO;"
        "genericArrayMethod" || "<A:Ljava/lang/Object;>([TA;)[TA;"
        "genericMethodWithUpperClassBound" || "<N:Ljava/lang/Number;>(TN;)TN;"
        "genericMethodWithUpperInterfaceBound" || "<S::Ljava/io/Serializable;>(TS;)TS;"
        "genericMethodWithUpperClassAndInterfaceBound" || "<S:Ljava/lang/Number;:Ljava/io/Serializable;>(TS;)TS;"
        "genericMethodWithTwoUpperInterfaceBounds" || "<S::Ljava/io/Serializable;:Ljava/lang/Appendable;>(TS;)TS;"
    }

    def "creates_method_signatures_with_nullable_annotation"(String method, String signature) {
        setup:
        Map<String, String> signatures = createMethodSignatures(NullableTest, ANNOTATED)

        expect:
        signatures.getAt(method) == signature

        where:
        method || signature
        "method" || "()V"
        "methodWithIntParameter" || "(I)I"
        "methodWithParameter" || "(L0java/lang/Object;)L0java/lang/Object;"
        "methodWithArrayParameter" || "([0Ljava/lang/String;)[0Ljava/lang/String;"
        "methodWithGenericParameter" || "(L0java/util/List<Ljava/lang/String;>;)L0java/util/List<Ljava/lang/String;>;"
        "methodWithWildcardParameter" || "(L0java/util/List<*>;)L0java/util/List<*>;"
        "methodWithUpperBoundedParameter" || "(L0java/util/List<+Ljava/io/Serializable;>;)L0java/util/List<+Ljava/io/Serializable;>;"
        "methodWithLowerBoundedParameter" || "(L0java/util/Comparator<-Ljava/lang/String;>;)L0java/util/Comparator<-Ljava/lang/String;>;"
        "genericMethod" || "<O:Ljava/lang/Object;>(T0O;)T0O;"
        "genericArrayMethod" || "<A:Ljava/lang/Object;>([0TA;)[0TA;"
        "genericMethodWithUpperClassBound" || "<N:Ljava/lang/Number;>(T0N;)T0N;"
        "genericMethodWithUpperInterfaceBound" || "<S::Ljava/io/Serializable;>(T0S;)T0S;"
        "genericMethodWithUpperClassAndInterfaceBound" || "<S:Ljava/lang/Number;:Ljava/io/Serializable;>(T0S;)T0S;"
        "genericMethodWithTwoUpperInterfaceBounds" || "<S::Ljava/io/Serializable;:Ljava/lang/Appendable;>(T0S;)T0S;"
    }

    def "creates_method_signatures_with_nonull_annotation"(String method, String signature) {
        setup:
        Map<String, String> signatures = createMethodSignatures(NonnullTest, ANNOTATED)

        expect:
        signatures.getAt(method) == signature

        where:
        method || signature
        "method" || "()V"
        "methodWithIntParameter" || "(I)I"
        "methodWithParameter" || "(L1java/lang/Object;)L1java/lang/Object;"
        "methodWithArrayParameter" || "([1Ljava/lang/String;)[1Ljava/lang/String;"
        "methodWithGenericParameter" || "(L1java/util/List<Ljava/lang/String;>;)L1java/util/List<Ljava/lang/String;>;"
        "methodWithWildcardParameter" || "(L1java/util/List<*>;)L1java/util/List<*>;"
        "methodWithUpperBoundedParameter" || "(L1java/util/List<+Ljava/io/Serializable;>;)L1java/util/List<+Ljava/io/Serializable;>;"
        "methodWithLowerBoundedParameter" || "(L1java/util/Comparator<-Ljava/lang/String;>;)L1java/util/Comparator<-Ljava/lang/String;>;"
        "genericMethod" || "<O:Ljava/lang/Object;>(T1O;)T1O;"
        "genericArrayMethod" || "<A:Ljava/lang/Object;>([1TA;)[1TA;"
        "genericMethodWithUpperClassBound" || "<N:Ljava/lang/Number;>(T1N;)T1N;"
        "genericMethodWithUpperInterfaceBound" || "<S::Ljava/io/Serializable;>(T1S;)T1S;"
        "genericMethodWithUpperClassAndInterfaceBound" || "<S:Ljava/lang/Number;:Ljava/io/Serializable;>(T1S;)T1S;"
        "genericMethodWithTwoUpperInterfaceBounds" || "<S::Ljava/io/Serializable;:Ljava/lang/Appendable;>(T1S;)T1S;"
    }

    def "creates_method_signatures_with_nullable_default"() {
        when:
        Map<String, String> signatures = createMethodSignatures(Test, NULLABLE_PARAMETERS)

        then:
        signatures.getAt("methodWithParameter") == "(L0java/lang/Object;)Ljava/lang/Object;"
    }

    def "creates_method_signatures_with_nullable_annotation_and_nullable_default"() {
        when:
        Map<String, String> signatures = createMethodSignatures(NullableTest, NULLABLE_PARAMETERS.override(ANNOTATED))

        then:
        signatures.getAt("methodWithParameter") == "(L0java/lang/Object;)L0java/lang/Object;"
    }

    def "creates_method_signatures_with_nonull_annotation_and_nullable_default"() {
        when:
        Map<String, String> signatures = createMethodSignatures(NonnullTest, NULLABLE_PARAMETERS.override(ANNOTATED))

        then:
        signatures.getAt("methodWithParameter") == "(L1java/lang/Object;)L1java/lang/Object;"
    }

    def "creates_method_signatures_with_ommitted_nullability"() {
        when:
        Map<String, String> signatures = createMethodSignatures(NullableTest, Nullability.omit())

        then:
        signatures.getAt("methodWithParameter") == "(Ljava/lang/Object;)Ljava/lang/Object;"
    }

    def "creates_method_signatures_for_generic_classes"(

            String method, String signature) {
        setup:
        Map<String, String> signatures = createMethodSignatures(GenericTest, Nullability.undefined())

        expect:
        signatures.getAt(method) == signature

        where:
        method || signature
        "method" || "()V"
        "methodWithIntParameter" || "(I)I"
        "methodWithParameter" || "(Ljava/lang/Object;)Ljava/lang/Object;"
        "methodWithArrayParameter" || "([Ljava/lang/String;)[Ljava/lang/String;"
        "methodWithVariableParameter" || "(TT;)TT;"
        "methodWithGenericParameter" || "(Ljava/util/List<Ljava/lang/String;>;)Ljava/util/List<Ljava/lang/String;>;"
        "methodWithWildcardParameter" || "(Ljava/util/List<*>;)Ljava/util/List<*>;"
        "methodWithUpperBoundedParameter" || "(Ljava/util/List<+Ljava/io/Serializable;>;)Ljava/util/List<+Ljava/io/Serializable;>;"
        "methodWithLowerBoundedParameter" || "(Ljava/util/Comparator<-Ljava/lang/String;>;)Ljava/util/Comparator<-Ljava/lang/String;>;"
        "genericMethod" || "<O:Ljava/lang/Object;>(TO;)TO;"
        "genericArrayMethod" || "<A:Ljava/lang/Object;>([TA;)[TA;"
        "genericMethodWithUpperClassBound" || "<N:Ljava/lang/Number;>(TN;)TN;"
        "genericMethodWithUpperInterfaceBound" || "<S::Ljava/io/Serializable;>(TS;)TS;"
        "genericMethodWithUpperClassAndInterfaceBound" || "<S:Ljava/lang/Number;:Ljava/io/Serializable;>(TS;)TS;"
        "genericMethodWithTwoUpperInterfaceBounds" || "<S::Ljava/io/Serializable;:Ljava/lang/Appendable;>(TS;)TS;"
        "methodWithNestedGenericParameter" || "<U:Ljava/lang/Object;>(L${GENERIC_TEST_CLASS_NAME}<TT;>.${NestedGenericTest.simpleName}<TU;>;)L${GENERIC_TEST_CLASS_NAME}<TT;>.${NestedGenericTest.simpleName}<TU;>;"
    }

    def "creates_method_signatures_for_nested_generic_classes"() {
        when:
        Map<String, String> signatures = createMethodSignatures(NestedGenericTest, Nullability.undefined())

        then:
        signatures.getAt("methodWithVariableParameter") == "(TT;TU;)TU;"
    }
}
