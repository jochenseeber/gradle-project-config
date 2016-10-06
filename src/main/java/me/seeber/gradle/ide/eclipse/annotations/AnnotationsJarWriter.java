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

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import com.google.common.base.Charsets;
import com.google.common.io.Closeables;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeDescription.Generic;
import net.bytebuddy.description.type.TypeList;

/**
 * Writer for Eclipse external annotations JARs
 */
public class AnnotationsJarWriter implements Closeable {

    /**
     * Underlying JAR output stream to write to
     */
    private final JarOutputStream jar;

    /**
     * Output stream writer to write to
     *
     * Encapsulates {@link #jar}. Use this stream to write text output.
     */
    private final OutputStreamWriter out;

    /**
     * Signature writer for signatures without nullability information
     */
    private final MethodSignatureWriter signatureWriter = new MethodSignatureWriter(Nullability.omit());

    /**
     * Nullability to use
     *
     * When writing signatures with nullability informtion, this is used to determine of an element is nullable or not.
     */
    private Nullability nullability;

    /**
     * Create a new JAR writer
     *
     * @param jarFile JAR file to write
     * @param nullability Nullability to use
     * @throws IOException if something goes horribly wrong...
     */
    public AnnotationsJarWriter(File jarFile, Nullability nullability) throws IOException {
        this.nullability = nullability;
        this.jar = new JarOutputStream(new FileOutputStream(jarFile));
        this.out = new OutputStreamWriter(this.jar, Charsets.UTF_8);
    }

    /**
     * Write a type description
     *
     * @param type Type to write
     * @param errorHandler Error handler for exceptions
     * @return <code>true</code> if any nullability information was written
     * @throws IOException if something bad happens...
     */
    public boolean write(TypeDescription type, Consumer<Exception> errorHandler) throws IOException {
        MethodSignatureWriter annotatedSignatureWriter = new MethodSignatureWriter(this.nullability);
        StringBuilder buf = new StringBuilder();

        boolean annotated = appendTypeSignature(buf, "class", type.asGenericType(), annotatedSignatureWriter);

        try {
            Generic superClass = type.getSuperClass();

            if (superClass != null && !superClass.represents(Object.class)) {
                if (appendTypeSignature(buf, "super", superClass, annotatedSignatureWriter)) {
                    annotated = true;
                }
            }

            for (Generic interfaceType : type.getInterfaces()) {
                if (appendTypeSignature(buf, "super", interfaceType, annotatedSignatureWriter)) {
                    annotated = true;
                }
            }
        }
        catch (Exception e) {
            throw new RuntimeException(String.format("Could not write external annotations for %s", type));
        }

        for (MethodDescription method : type.getDeclaredMethods()) {
            try {
                String plainSignature = this.signatureWriter.appendMethodSignature(method, new StringBuilder())
                        .toString();
                String annotatedSignature = annotatedSignatureWriter.appendMethodSignature(method, new StringBuilder())
                        .toString();

                if (!plainSignature.equals(annotatedSignature)) {
                    buf.append(method.getName()).append("\n");
                    buf.append(' ').append(plainSignature).append("\n");
                    buf.append(' ').append(annotatedSignature).append("\n");

                    annotated = true;
                }
            }
            catch (Exception e) {
                errorHandler.accept(e);
            }
        }

        if (annotated) {
            this.jar.putNextEntry(new JarEntry(type.getTypeName().replace('.', '/') + ".eea"));
            this.out.append(buf);
            this.out.flush();
        }

        return annotated;
    }

    /**
     * Append a type signature
     *
     * @param buf Buffer to append to
     * @param role Role (class or super)
     * @param type Type to append signature of
     * @param annotatedSignatureWriter Signature writer
     * @return <code>true</code> if any nullability information was written
     * @throws IOException if we stumble and fall...
     */
    protected boolean appendTypeSignature(StringBuilder buf, String role, Generic type,
            MethodSignatureWriter annotatedSignatureWriter) throws IOException {
        boolean annotated = false;

        buf.append(role).append(' ').append(type.asErasure().getInternalName()).append("\n");

        TypeList.Generic typeVariables = type.asErasure().getTypeVariables();

        if (!typeVariables.isEmpty()) {
            if (appendTypeParameters(buf, typeVariables, annotatedSignatureWriter)) {
                annotated = true;
            }
        }

        return annotated;
    }

    /**
     * Append type parameters
     *
     * @param buf Buffer to append to
     * @param typeParameters Type parameters
     * @param annotatedSignatureWriter Signature writer
     * @return <code>true</code> if any nullability information was written
     * @throws IOException if we fail completely...
     */
    protected boolean appendTypeParameters(StringBuilder buf, TypeList.Generic typeParameters,
            MethodSignatureWriter annotatedSignatureWriter) throws IOException {
        String plainTypeVariables = this.signatureWriter.appendTypeParameters(typeParameters, new StringBuilder())
                .toString();
        String annotatedTypeVariables = annotatedSignatureWriter
                .appendTypeParameters(typeParameters, new StringBuilder()).toString();
        buf.append(plainTypeVariables).append("\n");
        buf.append(annotatedTypeVariables).append("\n");
        boolean annotated = !plainTypeVariables.equals(annotatedTypeVariables);
        return annotated;
    }

    /**
     * Append type arguments
     *
     * @param buf Buffer to append to
     * @param typeArguments Type arguments
     * @return <code>true</code> if any nullability information was written
     * @throws IOException I'm sorry, this usually doesn't happen to me...
     */
    protected boolean appendTypeArguments(StringBuilder buf, TypeList.Generic typeArguments) throws IOException {
        String plainTypeVariables = this.signatureWriter.appendTypeArguments(typeArguments, new StringBuilder())
                .toString();
        String annotatedTypeVariables = this.signatureWriter.appendTypeArguments(typeArguments, new StringBuilder())
                .toString();
        buf.append(plainTypeVariables).append("\n");
        buf.append(annotatedTypeVariables).append("\n");
        boolean annotated = plainTypeVariables.equals(annotatedTypeVariables);
        return annotated;
    }

    /**
     * @see java.io.Closeable#close()
     */
    @Override
    public void close() throws IOException {
        Closeables.close(this.jar, false);
    }

}
