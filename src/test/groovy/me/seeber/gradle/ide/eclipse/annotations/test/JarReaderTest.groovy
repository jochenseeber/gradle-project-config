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
import java.util.jar.JarOutputStream

import org.junit.Rule
import org.junit.rules.TemporaryFolder

import com.google.common.base.Charsets
import com.google.common.io.Resources

import groovy.transform.TypeChecked
import me.seeber.gradle.ide.eclipse.annotations.JarReader
import me.seeber.gradle.ide.eclipse.test.jar.GenericTest
import me.seeber.gradle.ide.eclipse.test.jar.NonnullTest
import me.seeber.gradle.ide.eclipse.test.jar.NullableTest
import me.seeber.gradle.ide.eclipse.test.jar.Test
import net.bytebuddy.description.type.TypeDefinition
import spock.lang.Specification

@TypeChecked
class JarReaderTest extends Specification {

    @Rule
    TemporaryFolder temporaryFolder

    File createTestJar(boolean addPackageInfo) {
        File jarFile = temporaryFolder.newFile("test.jar")

        List<Class<?>> testClasses = [
            Test,
            GenericTest,
            NullableTest,
            NonnullTest,
        ]

        new JarOutputStream(new FileOutputStream(jarFile)).withStream { JarOutputStream out ->
            testClasses.each { Class<?> clazz ->
                String fileName = clazz.name.replaceAll("\\.", File.separator) + ".class"
                URL url = Resources.getResource(fileName)

                out.putNextEntry(new JarEntry(fileName))
                url.withInputStream { InputStream input -> out << input }
                out.closeEntry()
            }

            if(addPackageInfo) {
                Package pakkage = me.seeber.gradle.ide.eclipse.test.jar.Test.getPackage()
                String fileName = pakkage.name.replaceAll("\\.", File.separator) + "/package-info.class"
                URL url = Resources.getResource(fileName)

                out.putNextEntry(new JarEntry(fileName))
                url.withInputStream { InputStream input -> out << input }
                out.closeEntry()
            }

            out.putNextEntry(new JarEntry("test.properties"))
            out.write("test=true\n".getBytes(Charsets.UTF_8))
            out.closeEntry()
        }

        return jarFile
    }

    def "scans_jar"() {
        when:
        File jarFile = createTestJar(false)
        JarReader loader = new JarReader(jarFile, this.class.classLoader)

        then:
        loader.types.size() == 4

        cleanup:
        loader.close()
    }

    def "scans_jar_with_package_info"() {
        when:
        File jarFile = createTestJar(true)
        JarReader loader = new JarReader(jarFile, this.class.classLoader)
        TypeDefinition testClass = loader.types[Test.name]

        then:
        loader.types.size() == 4
        testClass != null
        testClass.getPackage() != null

        cleanup:
        loader.close()
    }
}
