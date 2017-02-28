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

import java.util.Optional;

import me.seeber.gradle.ide.eclipse.annotations.MethodSignatureWriter.Nullness;
import me.seeber.gradle.util.Validate;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.PackageDescription;

/**
 * Nullability specified by annotations
 */
public class AnnotationNullability implements Nullability {

    /**
     * Nullability specified by JSR-305 annotations
     */
    protected static final AnnotationNullability JSR_305 = new AnnotationNullability("javax.annotation.Nullable",
            "javax.annotation.Nonnull", "javax.annotation.ParametersAreNonnullByDefault");

    /**
     * Get Nullability that analyzes JSR-305 annotations
     *
     * @return Nullability that analyzes JSR-305 annotations
     */
    public static AnnotationNullability jsr305() {
        return JSR_305;
    }

    /**
     * Annotation that specifies an element is nullable
     */
    private final String nullableAnnotation;

    /**
     * Annotation that specifies an element is non-null
     */
    private final String nonnullAnnotation;

    /**
     * Package annotation that specified parameters are non-null by default
     */
    private final String nonnullParameterDefaultAnnotation;

    /**
     * Create a new nullability
     *
     * @param nullableAnnotation Annotation that specifies an element is nullable
     * @param nonnullAnnotation Annotation that specifies an element is non-null
     * @param nonnullParameterDefaultAnnotation Package annotation that specified parameters are non-null by default
     */
    public AnnotationNullability(String nullableAnnotation,
            String nonnullAnnotation,
            String nonnullParameterDefaultAnnotation) {
        this.nullableAnnotation = nullableAnnotation;
        this.nonnullAnnotation = nonnullAnnotation;
        this.nonnullParameterDefaultAnnotation = nonnullParameterDefaultAnnotation;
    }

    /**
     * @see me.seeber.gradle.ide.eclipse.annotations.Nullability#getParameterNullability(net.bytebuddy.description.method.ParameterDescription)
     */
    @Override
    public Nullness getParameterNullability(ParameterDescription parameter) {
        PackageDescription pakkage = parameter.getDeclaringMethod().getDeclaringType().asErasure().getPackage();
        Optional<AnnotationDescription> packageAnnotation = pakkage.getDeclaredAnnotations().stream()
                .filter(a -> a.getAnnotationType().getName().equals(this.nonnullParameterDefaultAnnotation)).findAny();
        Nullness defaultNullness = Validate
                .notNull(packageAnnotation.map(a -> Nullness.NONNULL).orElse(Nullness.UNDEFINED));

        Nullness nullability = getNullability(parameter.getDeclaredAnnotations());
        return defaultNullness.override(nullability);
    }

    /**
     * @see me.seeber.gradle.ide.eclipse.annotations.Nullability#getReturnValueNullability(net.bytebuddy.description.method.MethodDescription)
     */
    @Override
    public Nullness getReturnValueNullability(MethodDescription method) {
        Nullness nullability = getNullability(method.getDeclaredAnnotations());
        return nullability;
    }

    /**
     * Get the nullability from an annotation list
     * 
     * @param annotations Annotations to check
     * @return Nullness specified by annotations
     */
    protected Nullness getNullability(AnnotationList annotations) {
        Nullness nullability = Nullness.UNDEFINED;

        for (AnnotationDescription annotation : annotations) {
            if (annotation.getAnnotationType().getName().equals(this.nullableAnnotation)) {
                nullability = Nullness.NULLABLE;
            }
            else if (annotation.getAnnotationType().getName().equals(this.nonnullAnnotation)) {
                nullability = Nullness.NONNULL;
            }
        }

        return nullability;
    }

}
