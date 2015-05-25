package org.joeyb.freemapper.processor;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSet;

import org.joeyb.freemapper.FreeMapper;
import org.joeyb.freemapper.processor.exceptions.AnnotatedTypeUnsupportedException;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

@AutoService(javax.annotation.processing.Processor.class)
public class Processor extends AbstractProcessor {

    private Analyzer analyzer;

    private Filer filer;

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return ImmutableSet.of(FreeMapper.class.getName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        analyzer = new Analyzer(processingEnv.getElementUtils(),
                                processingEnv.getMessager());

        filer = processingEnv.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(FreeMapper.class)) {
            try {
                Metadata metadata = analyzer.analyze(annotatedElement);

                CodeGenerator codeGenerator = new CodeGenerator(metadata);

                try {
                    Writer writer = filer.createSourceFile(
                        metadata.getPackageName() + "." + metadata.getMapperName(),
                        annotatedElement)
                        .openWriter();

                    writer.append(codeGenerator.generate());

                    writer.close();
                } catch (IOException ex) {
                    processingEnv.getMessager()
                        .printMessage(Diagnostic.Kind.ERROR,
                                      String.format("Failed creating source file %s",
                                                    metadata.getMapperName()),
                                      annotatedElement);
                }
            } catch (AnnotatedTypeUnsupportedException ex) {
                // The error has already been printed by the analyzer.
                System.out.println(String.format("%s annotation processor failed for element %s",
                                                 FreeMapper.class.getSimpleName(),
                                                 annotatedElement.getSimpleName().toString()));
            }
        }

        return false;
    }
}
