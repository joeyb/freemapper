package org.joeyb.freemapper.processor;

import static org.joeyb.freemapper.processor.TypeUtils.erasesToAnyOf;

import com.google.common.collect.ImmutableSet;

import org.joeyb.freemapper.Field;
import org.joeyb.freemapper.processor.exceptions.AnnotatedTypeUnsupportedException;

import java.util.Collection;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

public class PropertyAnalyzer {

    private static final Pattern GETTER_PATTERN = Pattern.compile("^(get|is)(.+)");
    private static final String IS_PREFIX = "is";

    private final Elements elements;

    private final Messager messager;

    private final TypeElement typeElement;

    public PropertyAnalyzer(Elements elements,
                            Messager messager,
                            TypeElement typeElement) {
        this.elements = elements;
        this.messager = messager;
        this.typeElement = typeElement;
    }

    public Collection<Property> getProperties() throws AnnotatedTypeUnsupportedException {
        final ImmutableSet<ExecutableElement> methods = methodsOn(typeElement);

        return methods.stream()
            .map(m -> getProperty(m))
            .filter(p -> p.isPresent())
            .map(p -> p.get())
            .collect(Collectors.toList());
    }

    private Optional<MatchResult> getGetterNameMatchResult(ExecutableElement method) {
        String name = method.getSimpleName().toString();

        Matcher matcher = GETTER_PATTERN.matcher(name);

        if (!matcher.matches()) {
            // Ignore methods that are not getters
            return Optional.empty();
        }

        String prefix = matcher.group(1);
        String suffix = matcher.group(2);

        if (!Character.isUpperCase(suffix.codePointAt(0))) {
            printError(method,
                       "Getters are expected to contain an uppercase character after the prefix.");

            return Optional.empty();
        }

        TypeMirror returnType = getReturnType(method);

        if (returnType.getKind() == TypeKind.VOID) {
            printError(method, "Getter methods must not return void.");

            return Optional.empty();
        }

        if (prefix.equals(IS_PREFIX) && !isTypeBoolean(returnType)) {
            printError(method, "Getters with prefix '%s' must be return a boolean.", IS_PREFIX);

            return Optional.empty();
        }

        if (!method.getParameters().isEmpty()) {
            printError(method, "Getters should not accept parameters.");

            return Optional.empty();
        }

        return Optional.of(matcher.toMatchResult());
    }

    private Optional<Property> getProperty(ExecutableElement method) {
        Optional<MatchResult> getterNameMatchResult = getGetterNameMatchResult(method);

        if (!getterNameMatchResult.isPresent()) {
            return Optional.empty();
        }

        String name = getterNameMatchResult.get().group(2);
        TypeMirror propertyType = getReturnType(method);

        Field field = method.getAnnotation(Field.class);

        return Optional.of(new Property.Builder()
                               .setField(field == null ? name : field.name())
                               .setName(name)
                               .setGetterName(method.getSimpleName().toString())
                               .setType(propertyType)
                               .build());
    }

    private TypeMirror getReturnType(ExecutableElement method) {
        return method.getReturnType();
    }

    private boolean isTypeBoolean(TypeMirror type) {
        if (type.getKind() == TypeKind.BOOLEAN) return true;

        if (type.getKind() == TypeKind.DECLARED) {
            DeclaredType declaredType = (DeclaredType) type;

            return erasesToAnyOf(declaredType, Boolean.class);
        }

        return false;
    }

    private ImmutableSet<ExecutableElement> methodsOn(TypeElement type)
        throws AnnotatedTypeUnsupportedException {

        try {
            return org.inferred.freebuilder.processor.MethodFinder.methodsOn(type, elements);
        } catch (Exception ex) {
            throw new AnnotatedTypeUnsupportedException();
        }
    }

    private void printError(Element e, String msg, Object... args) {
        messager.printMessage(
            Diagnostic.Kind.ERROR,
            String.format(msg, args),
            e);
    }
}
