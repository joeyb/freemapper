package org.joeyb.freemapper.processor;

import static org.joeyb.freemapper.processor.TypeUtils.erasesToAnyOf;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.function.Supplier;

import javax.lang.model.element.Modifier;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

class CodeGenerator {

    private static final String SETTER_PREFIX = "set";

    private final Metadata metadata;

    public CodeGenerator(Metadata metadata) {
        this.metadata = metadata;
    }

    public String generate() {
        MethodSpec map = MethodSpec.methodBuilder("map")
            .addParameter(ResultSet.class, "rs")
            .returns(TypeName.get(metadata.getElement().asType()))
            .addException(SQLException.class)
            .addCode(getMapCodeBlock())
            .build();

        TypeVariableName t = TypeVariableName.get("T");

        TypeSpec resultSetSupplier = TypeSpec.interfaceBuilder("ResultSetSupplier")
            .addTypeVariable(t)
            .addAnnotation(FunctionalInterface.class)
            .addMethod(MethodSpec.methodBuilder("get")
                           .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                           .addException(SQLException.class)
                           .returns(t)
                           .build())
            .addModifiers(Modifier.PRIVATE)
            .build();

        MethodSpec getOptionalValue = MethodSpec.methodBuilder("getOptionalValue")
            .addTypeVariable(t)
            .addParameter(ResultSet.class, "rs")
            .addParameter(ParameterizedTypeName.get(ClassName.bestGuess("ResultSetSupplier"), t),
                          "getValue")
            .returns(ParameterizedTypeName.get(ClassName.get(Optional.class), t))
            .addException(SQLException.class)
            .addCode(
                CodeBlock.builder()
                    .addStatement("$T value = getValue.get()", t)
                    .addStatement("return rs.wasNull() ? Optional.empty() : Optional.of(value)")
                    .build())
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .build();

        TypeSpec mapper = TypeSpec.classBuilder(metadata.getMapperName())
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addMethod(map)
            .addMethod(getOptionalValue)
            .addType(resultSetSupplier)
            .build();

        JavaFile file = JavaFile.builder(metadata.getPackageName(), mapper)
            .build();

        return file.toString();
    }

    private void addMapCodeBlockPropertyStatement(CodeBlock.Builder builder, Property property) {
        Optional<String> resultSetGetter = getResultSetGetter(property.getType());

        if (!resultSetGetter.isPresent()) {
            // Check to see if the property is Optional. If so, handle it differently.
            Optional<TypeMirror> optionalType = getGenericTypeFromOptional(property.getType());

            if (!optionalType.isPresent()) {
                return;
            }

            resultSetGetter = getResultSetGetter(optionalType.get());

            if (resultSetGetter.isPresent()) {
                builder.addStatement("b.$L(getOptionalValue(rs, () -> rs.$L($S)))",
                                     SETTER_PREFIX + property.getName(),
                                     resultSetGetter.get(),
                                     property.getField());
            }

            return;
        }

        builder.addStatement("b.$L(rs.$L($S))",
                             SETTER_PREFIX + property.getName(),
                             resultSetGetter.get(),
                             property.getField());
    }

    private Optional<TypeMirror> getGenericTypeFromOptional(TypeMirror type) {
        if (type.getKind() != TypeKind.DECLARED) {
            return Optional.empty();
        }

        DeclaredType declaredType = (DeclaredType) type;

        if (!erasesToAnyOf(declaredType, Optional.class)
            || declaredType.getTypeArguments().size() != 1) {
            return Optional.empty();
        }

        return Optional.of(declaredType.getTypeArguments().get(0));
    }

    private CodeBlock getMapCodeBlock() {
        CodeBlock.Builder builder = CodeBlock.builder();

        TypeName builderTypeName = TypeName.get(metadata.getBuilderElement().asType());

        builder.addStatement("$T b = new $T()", builderTypeName, builderTypeName);

        metadata.getProperties().stream()
            .forEach(p -> addMapCodeBlockPropertyStatement(builder, p));

        builder.addStatement("return b.build()");

        return builder.build();
    }

    private Optional<String> getResultSetGetter(TypeMirror type) {
        switch (type.getKind()) {
        case DECLARED:
            DeclaredType declaredType = (DeclaredType) type;

            if (erasesToAnyOf(declaredType, String.class)) {
                return Optional.of("getString");
            }
            if (erasesToAnyOf(declaredType, Boolean.class)) {
                return Optional.of("getBoolean");
            }
            if (erasesToAnyOf(declaredType, Byte.class)) {
                return Optional.of("getByte");
            }
            if (erasesToAnyOf(declaredType, Short.class)) {
                return Optional.of("getShort");
            }
            if (erasesToAnyOf(declaredType, Integer.class)) {
                return Optional.of("getInt");
            }
            if (erasesToAnyOf(declaredType, Long.class)) {
                return Optional.of("getLong");
            }
            if (erasesToAnyOf(declaredType, Float.class)) {
                return Optional.of("getFloat");
            }
            if (erasesToAnyOf(declaredType, Double.class)) {
                return Optional.of("getDouble");
            }

            // TODO: Handle Dates
            return Optional.empty();
        case BOOLEAN:
            return Optional.of("getBoolean");
        case BYTE:
            return Optional.of("getByte");
        case SHORT:
            return Optional.of("getShort");
        case INT:
            return Optional.of("getInt");
        case LONG:
            return Optional.of("getLong");
        case FLOAT:
            return Optional.of("getFloat");
        case DOUBLE:
            return Optional.of("getDouble");
        default:
            return Optional.empty();
        }
    }
}
