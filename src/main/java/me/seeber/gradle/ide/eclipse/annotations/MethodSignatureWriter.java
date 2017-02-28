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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;

import com.google.common.collect.Lists;

import me.seeber.gradle.util.Validate;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.PackageDescription;
import net.bytebuddy.description.type.TypeDefinition.Sort;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeDescription.Generic;
import net.bytebuddy.description.type.TypeList;

/**
 * Writer for Java method signatures
 *
 * This class writes method signatures as required by the Eclipse external annotation format. It closely follows the the
 * Java Virtual Machine Specification's grammar for method signatures.
 *
 * @see <a href="https://wiki.eclipse.org/JDT_Core/Null_Analysis/External_Annotations">External Annotations</a>
 * @see <a href="http://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.7.9.1">JVMS Signatures</a>
 */
public class MethodSignatureWriter {

    /**
     * Values used to write nullness in signatures
     */
    public enum Nullness {

        /**
         * Element may be <code>null</code>
         */
        NULLABLE("0"),

        /**
         * Element is never <code>null</code>
         */
        NONNULL("1"),

        /**
         * Nullness is unknown
         */
        UNDEFINED(""),

        /**
         * Nulness should be omitted
         */
        OMIT("") {
            @Override
            public Nullness override(Nullness nullable) {
                return this;
            }
        };

        /**
         * Marker to write nullness in signatures
         *
         * This is either "0", "1" or the empty string, depending on the nullness
         */
        private final String marker;

        /**
         * Create a new nullness
         *
         * @param marker Marker to write nullness in signatures
         */
        private Nullness(String marker) {
            this.marker = marker;
        }

        /**
         * Get the marker to write nullness in signatures
         *
         * @return Marker to write nullness in signatures
         */
        public String getMarker() {
            return this.marker;
        }

        /**
         * Override this nullness value with another value
         *
         * @param other Override vale
         * @return This nullness value overriden with the supplied value
         */
        public Nullness override(Nullness other) {
            return other;
        }
    }

    /**
     * Nullability used to write signatures
     */
    private final Nullability nullability;

    /**
     * Create a new method signature writer
     *
     * @param nullability Nullability used to write signatures
     */
    public MethodSignatureWriter(Nullability nullability) {
        this.nullability = Validate.notNull(nullability);
    }

    /**
     * Append a type
     *
     * @param type Type to append
     * @param nullable Nullness of type
     * @param output Output to append to
     * @param <A> Type of output
     * @return Supplied output to append to
     */
    public <A extends Appendable> A appendJavaTypeSignature(Generic type, Nullness nullable, A output) {
        try {
            if (type.isPrimitive()) {
                appendBaseType(type, output);
            }
            else {
                appendReferenceTypeSignature(type, nullable, output);
            }

            return output;
        }
        catch (Exception e) {
            throw new RuntimeException(String.format("Could not write java type signature %s", type));
        }
    }

    /**
     * Append a base type
     *
     * @param type Type to append
     * @param output Output to append to
     * @param <A> Type of output
     * @return Supplied output to append to
     */
    public <A extends Appendable> A appendBaseType(Generic type, A output) {
        try {
            output.append(type.asErasure().getDescriptor());
            return output;
        }
        catch (Exception e) {
            throw new RuntimeException(String.format("Could not write class type signature %s", type));
        }
    }

    /**
     * Append a referency type
     *
     * @param type Type to append
     * @param nullable Nullness of type
     * @param output Output to append to
     * @param <A> Type of output
     * @return Supplied output to append to
     */
    public <A extends Appendable> A appendReferenceTypeSignature(Generic type, Nullness nullable, A output) {
        try {
            if (type.isArray()) {
                appendArrayTypeSignature(type, nullable, output);
            }
            else if (type.getSort() == Sort.VARIABLE) {
                appendTypeVariableSignature(type, nullable, output);
            }
            else {
                appendClassTypeSignature(type, nullable, output);
            }

            return output;
        }
        catch (Exception e) {
            throw new RuntimeException(String.format("Could not write reference type signature %s", type));
        }
    }

    /**
     * Append a class type
     *
     * @param type Type to append
     * @param nullable Nullness of type
     * @param output Output to append to
     * @param <A> Type of output
     * @return Supplied output to append to
     */
    public <A extends Appendable> A appendClassTypeSignature(Generic type, Nullness nullable, A output) {
        try {
            output.append('L').append(nullable.getMarker());

            PackageDescription pakkage = type.asErasure().getPackage();

            if (!pakkage.getName().isEmpty()) {
                appendPackageSpecifier(pakkage, output);
            }

            List<@NonNull Generic> ownerTypes = new ArrayList<>();

            for (Generic ownerType = type.getOwnerType(); ownerType != null; ownerType = ownerType.getOwnerType()) {
                ownerTypes.add(ownerType);
            }

            for (Generic ownerType : Lists.reverse(ownerTypes)) {
                appendSimpleClassTypeSignature(ownerType, output);
                output.append('.');
            }

            appendSimpleClassTypeSignature(type, output);

            output.append(';');

            return output;
        }
        catch (Exception e) {
            throw new RuntimeException(String.format("Could not write class type signature %s", type));
        }
    }

    /**
     * Append a simple class type
     *
     * @param type Type to append
     * @param output Output to append to
     * @param <A> Type of output
     * @return Supplied output to append to
     */
    public <A extends Appendable> A appendSimpleClassTypeSignature(Generic type, A output) {
        try {
            output.append(type.asErasure().getSimpleName());

            if (type.getSort() == Sort.PARAMETERIZED) {
                appendTypeArguments(type.getTypeArguments(), output);
            }

            return output;
        }
        catch (Exception e) {
            throw new RuntimeException(String.format("Could not write type signature %s", type));
        }
    }

    /**
     * Append type arguments
     *
     * @param typeArguments Type arguments to append
     * @param output Output to append to
     * @param <A> Type of output
     * @return Supplied output to append to
     */
    public <A extends Appendable> A appendTypeArguments(TypeList.Generic typeArguments, A output) {
        try {
            output.append('<');

            for (Generic typeArgument : typeArguments) {
                appendTypeArgument(typeArgument, output);
            }

            output.append('>');

            return output;
        }
        catch (Exception e) {
            throw new RuntimeException(String.format("Could not write type arguments %s", typeArguments));
        }
    }

    /**
     * Append a type Argument
     *
     * @param typeArgument Type argument to append
     * @param output Output to append to
     * @param <A> Type of output
     * @return Supplied output to append to
     */
    public <A extends Appendable> A appendTypeArgument(Generic typeArgument, A output) {
        try {
            if (typeArgument.getSort() == Sort.WILDCARD) {
                TypeList.Generic lowerBounds = typeArgument.getLowerBounds();
                TypeList.Generic upperBounds = typeArgument.getUpperBounds();

                if (lowerBounds.size() > 0) {
                    if (upperBounds.size() != 1 || !upperBounds.get(0).represents(Object.class)) {
                        throw new IllegalArgumentException(String.format(
                                "Lower bounded type argument '%s' must have Object as upper bound", typeArgument));
                    }

                    output.append('-');
                    appendReferenceTypeSignature(typeArgument.getLowerBounds().get(0), Nullness.UNDEFINED, output);
                }
                else if (upperBounds.size() > 0) {
                    if (upperBounds.size() > 1) {
                        throw new IllegalArgumentException(
                                String.format("Upper bounded type argument '%s' cannot have more than one upper bound",
                                        typeArgument));
                    }

                    if (upperBounds.get(0).represents(Object.class)) {
                        output.append('*');
                    }
                    else {
                        output.append('+');
                        appendReferenceTypeSignature(upperBounds.get(0), Nullness.UNDEFINED, output);
                    }
                }
            }
            else {
                appendReferenceTypeSignature(typeArgument, Nullness.UNDEFINED, output);
            }

            return output;
        }
        catch (Exception e) {
            throw new RuntimeException(String.format("Could not write type argument %s", typeArgument));
        }
    }

    /**
     * Append a package specifier
     *
     * @param pakkage Package specifier to append
     * @param output Output to append to
     * @param <A> Type of output
     * @return Supplied output to append to
     */
    public <A extends Appendable> A appendPackageSpecifier(PackageDescription pakkage, A output) {
        try {
            output.append(pakkage.getName().replace('.', '/'));
            output.append('/');
            return output;
        }
        catch (Exception e) {
            throw new RuntimeException(String.format("Could not package specifier %s", pakkage));
        }
    }

    /**
     * Append a type variable signature
     *
     * @param typeVariable Type variable to append signature
     * @param nullable Nullness of type variable
     * @param output Output to append to
     * @param <A> Type of output
     * @return Supplied output to append to
     */
    public <A extends Appendable> A appendTypeVariableSignature(Generic typeVariable, Nullness nullable, A output) {
        try {
            output.append('T').append(nullable.getMarker()).append(typeVariable.getTypeName()).append(';');
            return output;
        }
        catch (Exception e) {
            throw new RuntimeException(String.format("Could not type variable %s", typeVariable));
        }
    }

    /**
     * Append an array type signature
     *
     * @param type Array type to append
     * @param nullable Nullness of type
     * @param output Output to append to
     * @param <A> Type of output
     * @return Supplied output to append to
     */
    public <A extends Appendable> A appendArrayTypeSignature(Generic type, Nullness nullable, A output) {
        try {
            output.append('[').append(nullable.getMarker());
            appendJavaTypeSignature(Validate.notNull(type.getComponentType()), Nullness.UNDEFINED, output);
            return output;
        }
        catch (Exception e) {
            throw new RuntimeException(String.format("Could not write array type %s", type));
        }
    }

    /**
     * Append a method signature
     *
     * @param method Method to append signature of
     * @param output Output to append to
     * @param <A> Type of output
     * @return Supplied output to append to
     */
    public <A extends Appendable> A appendMethodSignature(MethodDescription method, A output) {
        try {
            if (!method.getTypeVariables().isEmpty()) {
                appendTypeParameters(method.getTypeVariables(), output);
            }

            output.append('(');

            for (ParameterDescription parameter : method.getParameters()) {
                Nullness nullable = this.nullability.getParameterNullability(parameter);
                appendJavaTypeSignature(parameter.getType(), nullable, output);
            }

            output.append(')');

            Nullness nullable = this.nullability.getReturnValueNullability(method);
            appendJavaTypeSignature(method.getReturnType(), nullable, output);

            return output;
        }
        catch (Exception e) {
            throw new RuntimeException(String.format("Could not write method signature %s", method));
        }
    }

    /**
     * Append type parameters
     *
     * @param typeParameters Type parameters to append
     * @param output Output to append to
     * @param <A> Type of output
     * @return Supplied output to append to
     */
    public <A extends Appendable> A appendTypeParameters(TypeList.Generic typeParameters, A output) {
        try {
            output.append('<');

            for (Generic typeArgument : typeParameters) {
                appendTypeParameter(typeArgument, output);
            }

            output.append('>');
            return output;
        }
        catch (Exception e) {
            throw new RuntimeException(String.format("Could not write type parameters %s", typeParameters));
        }
    }

    /**
     * Append a type parameter
     *
     * @param typeParameter Type parameters to append
     * @param output Output to append to
     * @param <A> Type of output
     * @return Supplied output to append to
     */
    public <A extends Appendable> A appendTypeParameter(Generic typeParameter, A output) {
        try {
            output.append(typeParameter.getSymbol());

            TypeList.Generic upperBounds = typeParameter.getUpperBounds();

            if (upperBounds.size() == 0) {
                throw new IllegalArgumentException(
                        String.format("Type parameter '%s' must have upper bounds", typeParameter));
            }

            if (upperBounds.get(0).isInterface()) {
                output.append(':');
            }

            for (Generic upperBound : upperBounds) {
                appendClassOrInterfaceBound(upperBound, output);
            }

            return output;
        }
        catch (Exception e) {
            throw new RuntimeException(String.format("Could not write type parameter %s", typeParameter));
        }
    }

    /**
     * Append a class or interface bound
     *
     * @param bound Bound to append
     * @param output Output to append to
     * @param <A> Type of output
     * @return Supplied output to append to
     */
    public <A extends Appendable> A appendClassOrInterfaceBound(Generic bound, A output) {
        try {
            output.append(':');

            appendReferenceTypeSignature(bound, Nullness.UNDEFINED, output);

            return output;
        }
        catch (Exception e) {
            throw new RuntimeException(String.format("Could write class or interface bound %s", bound));
        }
    }

    /**
     * Get a mangled name for a type
     *
     * @param type Type to get mangled name for
     * @return Mangled name for type
     */
    public String getMangledName(TypeDescription type) {
        return type.getCanonicalName().replace('.', '/');
    }

    /**
     * Get the nullabilty used to write signatures
     *
     * @return Nullabilty used to write signatures
     */
    protected Nullability getNullability() {
        return this.nullability;
    }

}
