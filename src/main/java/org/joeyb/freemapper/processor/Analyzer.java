package org.joeyb.freemapper.processor;

import static com.google.common.base.Preconditions.checkNotNull;

import org.joeyb.freemapper.FreeMapper;
import org.joeyb.freemapper.processor.exceptions.AnnotatedTypeUnsupportedException;

import java.util.Optional;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

public class Analyzer {

    private static final String MAPPER_SUFFIX = "_Mapper";

    private Elements elements;
    private Messager messager;

    Analyzer(Elements elements,
             Messager messager) {
        this.elements = elements;
        this.messager = messager;
    }

    public Metadata analyze(Element element) throws AnnotatedTypeUnsupportedException {
        checkNotNull(element);

        final TypeElement e = validateType(element);

        PropertyAnalyzer propertyAnalyzer = new PropertyAnalyzer(elements,
                                                                 messager,
                                                                 e);

        Optional<TypeElement> builderElement = e.getEnclosedElements().stream()
            .filter(el -> el.getKind() == ElementKind.CLASS
                          && el.getSimpleName().toString().equals("Builder"))
            .map(el -> (TypeElement) el)
            .findFirst();

        if (!builderElement.isPresent()) {
            printError(e, "Must have a nested builder class named 'Builder'");
            throw new AnnotatedTypeUnsupportedException();
        }

        return new Metadata.Builder()
            .setBuilderElement(builderElement.get())
            .setElement(e)
            .setMapperName(e.getSimpleName().toString() + MAPPER_SUFFIX)
            .setName(e.getSimpleName().toString())
            .setPackageName(elements.getPackageOf(e).getQualifiedName().toString())
            .addAllProperties(propertyAnalyzer.getProperties())
            .build();
    }

    private void printError(Element e, String msg, Object... args) {
        messager.printMessage(
            Diagnostic.Kind.ERROR,
            String.format(msg, args),
            e);
    }

    private TypeElement validateType(Element element) throws AnnotatedTypeUnsupportedException {
        ElementKind kind = element.getKind();

        if (kind != ElementKind.CLASS && kind != ElementKind.INTERFACE) {
            printError(element,
                       "Only classes and interfaces can be annotated with @%s",
                       FreeMapper.class.getSimpleName());

            throw new AnnotatedTypeUnsupportedException();
        }

        return (TypeElement) element;
    }
}
