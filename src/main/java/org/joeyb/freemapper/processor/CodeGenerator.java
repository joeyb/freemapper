package org.joeyb.freemapper.processor;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import javax.lang.model.element.Modifier;
import javax.lang.model.type.DeclaredType;

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

        TypeSpec mapper = TypeSpec.classBuilder(metadata.getMapperName())
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addMethod(map)
            .build();

        JavaFile file = JavaFile.builder(metadata.getPackageName(), mapper)
            .build();

        return file.toString();
    }

    private void addMapCodeBlockPropertyStatement(CodeBlock.Builder builder, Property property) {
        Optional<String> resultSetGetter = getResultSetGetter(property);

        if (!resultSetGetter.isPresent())
            return;

        builder.addStatement("b.$L(rs.$L($S))",
                             SETTER_PREFIX + property.getName(),
                             resultSetGetter.get(),
                             property.getField());
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

    private Optional<String> getResultSetGetter(Property property) {
        switch (property.getType().getKind()) {
        case DECLARED:
            DeclaredType type = (DeclaredType) property.getType();

            if (erasesToAnyOf(type, String.class)) {
                return Optional.of("getString");
            }

            // TODO: handle Dates, Optionals, etc.
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

    static boolean erasesToAnyOf(DeclaredType type, Class<?>... possibilities) {
        String erasedType = type.asElement().toString();

        for (Class<?> possibility : possibilities) {
            if (possibility.getName().equals(erasedType)) {
                return true;
            }
        }
        return false;
    }
}
