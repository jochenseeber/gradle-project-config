/*
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
package me.seeber.gradle.ide.eclipse.annotations.test

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.zip.ZipEntry

import org.junit.Rule
import org.junit.rules.TemporaryFolder

import com.google.common.base.Charsets

import groovy.transform.TypeChecked
import me.seeber.gradle.ide.eclipse.annotations.AnnotationNullability
import me.seeber.gradle.ide.eclipse.annotations.AnnotationsJarWriter
import me.seeber.gradle.ide.eclipse.annotations.Nullability
import me.seeber.gradle.ide.eclipse.test.jar.MaybeNil
import me.seeber.gradle.ide.eclipse.test.jar.NeverNil
import me.seeber.gradle.ide.eclipse.test.jar.NullableGenericTest
import me.seeber.gradle.ide.eclipse.test.jar.NullableTest
import me.seeber.gradle.ide.eclipse.test.jar.ParametersAreNeverNilByDefault
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.pool.TypePool
import spock.lang.Specification

@TypeChecked
@SuppressWarnings("javadoc")
class AnnotationsJarWriterTest extends Specification {

    protected static final Nullability ANNOTATED = new AnnotationNullability(MaybeNil.name, NeverNil.name, ParametersAreNeverNilByDefault.name)

    @Rule
    TemporaryFolder temporaryFolder

    JarFile createAnnotationJar(Class<?> type) {
        File jarFile = temporaryFolder.newFile("test.jar")
        TypePool typePool = TypePool.ClassLoading.of(this.class.classLoader)
        TypeDescription testType = typePool.describe(type.name).resolve()
        String testTypeName = testType.internalName
        AnnotationsJarWriter writer = new AnnotationsJarWriter(jarFile, ANNOTATED)
        writer.write(testType) { Exception e -> throw e }
        writer.close()
        new JarFile(jarFile)
    }

    def "writes_jar"() {
        when:
        String entryName = NullableTest.name.replaceAll("\\.", "/")
        JarFile jar = createAnnotationJar(NullableTest)

        then:
        ZipEntry entry = jar.getEntry("${entryName}.eea")
        entry != null
        entry instanceof JarEntry
        String content = jar.getInputStream(entry).getText(Charsets.UTF_8.name())
        content.startsWith("class ${entryName}".toString()) == true

        cleanup:
        jar.close()
    }

    def "writes_jar_with_generic_type"() {
        when:
        String entryName = NullableGenericTest.name.replaceAll("\\.", "/")
        JarFile jar = createAnnotationJar(NullableGenericTest)

        then:
        ZipEntry entry = jar.getEntry("${entryName}.eea")
        entry != null
        entry instanceof JarEntry
        String content = jar.getInputStream(entry).getText(Charsets.UTF_8.name())
        content.startsWith("class ${entryName}".toString()) == true

        cleanup:
        jar.close()
    }
}
