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
package me.seeber.gradle.ide.eclipse.annotations;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFiles;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;

import com.google.common.base.Throwables;
import com.google.common.io.Files;

import me.seeber.gradle.ide.eclipse.EclipseConfigPlugin;
import me.seeber.gradle.util.Validate;
import net.bytebuddy.description.type.TypeDescription;

/**
 * Task to generate Eclipse external annotations from a JAR
 *
 * This task scans the supplied JARs for nullability annotations and for each processed source JAR creates a JAR
 * containing external Eclipse nullability annotations in the destination directory.
 */
public class EclipseAnnotationsTask extends ConventionTask {

    /**
     * JARs to process
     */
    private Collection<File> jars;

    /**
     * Destination directory for created annotation JARs
     */
    private File destinationDir;

    /**
     * Create a new task
     */
    public EclipseAnnotationsTask() {
        this.jars = Collections.emptySet();
        this.destinationDir = new File(Project.DEFAULT_BUILD_DIR_NAME, "annotations");
    }

    /**
     * Get the JAR files to process
     *
     * @return JAR files to process
     */
    @InputFiles
    public Collection<File> getJars() {
        return this.jars;
    }

    /**
     * Set the JAR files to process
     *
     * @param jars JAR files to process
     */
    public void setJars(Collection<File> jars) {
        this.jars = jars;
    }

    /**
     * Get the destination directory for created annotation JARs
     *
     * @return Destination directory for created annotation JARs
     */
    @OutputDirectory
    public File getDestinationDir() {
        return this.destinationDir;
    }

    /**
     * Set the destination directory for created annotation JARs
     *
     * @param destinationDir Destination directory for created annotation JARs
     */
    public void setDestinationDir(File destinationDir) {
        this.destinationDir = destinationDir;
    }

    /**
     * Get the created annotation JARs
     *
     * @return Created annotation JARs
     */
    @OutputFiles
    public Set<File> getAnnotationJars() {
        return this.jars.stream().map(f -> getTargetFile(Validate.notNull(f)))
                .collect(Collectors.toCollection(() -> new HashSet<>()));
    }

    /**
     * Create the annotation JARs for the input JARs
     *
     * @param inputs Task inputs
     */
    @TaskAction
    public void createAnnotationJars(IncrementalTaskInputs inputs) {
        if (!inputs.isIncremental()) {
            for (File file : getAnnotationJars()) {
                file.delete();
            }
        }

        inputs.outOfDate(file -> {
            createAnnotationJar(file.getFile());
        });

        inputs.removed(file -> {
            file.getFile().delete();
        });
    }

    /**
     * Create an annotation JAR for a JAR file
     *
     * @param jarFile JAR file to process
     * @return <code>true</code> if nullability information was found
     */
    protected boolean createAnnotationJar(File jarFile) {
        File annotationJarFile = getTargetFile(jarFile);

        boolean result = withTempFile("annotations", ".zip", tempFile -> {
            try {
                boolean annotated = createAnnotationJar(jarFile, tempFile);

                if (annotated) {
                    Files.move(tempFile, annotationJarFile);
                }
                else {
                    Files.write(new byte[0], annotationJarFile);
                }

                return annotated;
            }
            catch (Exception e) {
                throw new GradleException(String.format("Could not read JAR '%s'.", jarFile), e);
            }
        });

        return result;
    }

    /**
     * Get the target file for an input file
     *
     * @param jarFile Source JAR file
     * @return Target file
     */
    protected File getTargetFile(File jarFile) {
        String baseName = Files.getNameWithoutExtension(jarFile.getName());
        File annotationJarFile = new File(getDestinationDir(), baseName + "-annotations.zip");
        return annotationJarFile;
    }

    /**
     * Create an annotation JAR file for a JAR file
     *
     * @param jarFile Source JAR file
     * @param annotationJarFile Target annotation JAR file
     * @return <code>true</code> if nullability information was found
     */
    protected boolean createAnnotationJar(File jarFile, File annotationJarFile) {
        getLogger().info("Creating annotations JAR for '{}'", jarFile);

        ClassLoader classLoader = new URLClassLoader(getClasspathUrls());
        Set<String> knownErrors = new HashSet<>();
        boolean annotated = false;

        try (AnnotationsJarWriter writer = new AnnotationsJarWriter(annotationJarFile, AnnotationNullability.jsr305());
                JarReader reader = new JarReader(jarFile, classLoader)) {
            List<TypeDescription> types = new ArrayList<>(reader.getTypes().values());
            types.sort((a, b) -> a.getName().compareTo(b.getName()));

            for (TypeDescription type : types) {
                getLogger().debug("Adding annotations for type {}", type.getName());

                try {
                    boolean typeAnnotated = writer.write(type, e -> {
                        if (knownErrors.add(e.getMessage())) {
                            getLogger().info(e.getMessage());
                        }
                    });

                    annotated = annotated || typeAnnotated;
                }
                catch (Exception e) {
                    String message = Throwables.getCausalChain(e).stream().map(t -> t.getMessage())
                            .collect(Collectors.joining(": "));
                    getLogger().error(message);
                }
            }
        }
        catch (IOException e) {
            throw new GradleException(String.format("Could note create annotation JAR file for '%s'", jarFile));
        }

        return annotated;
    }

    /**
     * Perform an action with a supplied temporary file
     *
     * @param prefix Prefix of temporary file
     * @param suffix Suffix of temporary file
     * @param action Action to perform
     * @param <T> Result type of action
     * @return Result of the action
     */
    protected <T> T withTempFile(String prefix, String suffix, Function<@NonNull File, T> action) {
        File tempFile = null;

        try {
            tempFile = File.createTempFile(prefix, suffix);

            T result = action.apply(tempFile);
            return result;
        }
        catch (IOException e) {
            Throwables.throwIfUnchecked(e);
            throw new RuntimeException(e);
        }
        finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    /**
     * Get the URLs for the classpath during annotation processing
     *
     * @return Classpath URLs
     */
    @Input
    protected URL[] getClasspathUrls() {
        Configuration compileConfiguration = getProject().getConfigurations()
                .getByName(EclipseConfigPlugin.ECLIPSE_ANNOTATIONS_CONFIGURATION);

        getLogger().info("Using classpath '{}'", compileConfiguration.getAsPath());

        URL[] urls = compileConfiguration.getFiles().stream().map(f -> {
            try {
                return f.toURI().toURL();
            }
            catch (MalformedURLException e) {
                throw new GradleException(
                        String.format("Could not create classpath for annotations task %s.", getName()), e);
            }
        }).toArray(s -> new URL[s]);

        return urls;
    }
}
