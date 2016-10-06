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

import static java.lang.String.format;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Closeables;
import com.google.common.io.Files;

import me.seeber.gradle.util.Validate;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.pool.TypePool.CacheProvider;
import net.bytebuddy.pool.TypePool.Default.ReaderMode;

/**
 * Scan a dependency for nullability annotations
 *
 * @see <a href="https://wiki.eclipse.org/JDT_Core/Null_Analysis/External_Annotations">Eclipse JDT External
 *      Annotations</a>
 */
public class JarReader implements Closeable {

    /**
     * Class loader to resolve types
     */
    private final ClassLoader classLoader;

    /**
     * Class file locator to search for class files
     */
    private @Nullable ClassFileLocator classFileLocator;

    /**
     * Loaded types
     */
    private @Nullable Map<@NonNull String, @NonNull TypeDescription> types = new HashMap<>();

    /**
     * Create a new AnnotationsJarBuilder
     *
     * @param jarFile JAR file to read
     * @param classLoader Class loader to resolve types
     */
    public JarReader(File jarFile, ClassLoader classLoader) {
        this.classLoader = classLoader;

        try {
            openJarFile(jarFile);
        }
        catch (IOException e) {
            throw new RuntimeException("Cannot create annotations JAR builder", e);
        }
    }

    /**
     * Open a JAR file for reading
     * 
     * @param jarFile JAR file for reading
     * @throws IOException I'm so sorry...
     */
    protected void openJarFile(File jarFile) throws IOException {
        ClassFileLocator classFileLocator = null;

        try {
            List<String> classFileNames = new ArrayList<>();

            try (JarFile jar = new JarFile(jarFile)) {
                for (JarEntry entry : Collections.list(jar.entries())) {
                    if (!entry.isDirectory() && Files.getFileExtension(entry.getName()).equals("class")) {
                        classFileNames.add(entry.getName());
                    }
                }
            }
            catch (IOException e) {
                throw new RuntimeException(format("Could not scan jar file '%s' for contained classes", jarFile), e);
            }

            classFileLocator = ClassFileLocator.ForJarFile.of(jarFile);
            TypePool parentTypePool = TypePool.Default.of(getClassLoader());
            TypePool typePool = new TypePool.Default.WithLazyResolution(CacheProvider.Simple.withObjectType(),
                    classFileLocator, ReaderMode.FAST, parentTypePool);
            Map<@NonNull String, @NonNull TypeDescription> types = new HashMap<>();

            for (String classFileName : classFileNames) {
                String className = Files.getNameWithoutExtension(classFileName.replace(File.separatorChar, '.'));
                TypeDescription type = typePool.describe(className).resolve();

                if (!type.isPackageType()) {
                    types.put(type.getName(), type);
                }
            }

            this.classFileLocator = classFileLocator;
            this.types = ImmutableMap.copyOf(types);
        }
        catch (Exception e) {
            Closeables.close(classFileLocator, true);
            throw new RuntimeException(String.format("Could not scan JAR file '%s'", jarFile));
        }
    }

    /**
     * Get the loaded types
     *
     * @return types Loaded types
     */
    public Map<String, TypeDescription> getTypes() {
        return Validate.notNull(this.types);
    }

    /**
     * Get the class loader
     *
     * @return classLoader Class loader
     */
    public ClassLoader getClassLoader() {
        return this.classLoader;
    }

    /**
     * @see java.lang.AutoCloseable#close()
     */
    @Override
    public void close() throws IOException {
        this.types = null;
        Closeables.close(this.classFileLocator, false);
    }

}
