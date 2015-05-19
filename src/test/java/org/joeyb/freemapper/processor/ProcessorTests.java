package org.joeyb.freemapper.processor;

import static com.google.common.truth.TruthJUnit.assume;
import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;

import com.google.testing.compile.JavaFileObjects;

import org.junit.Test;

public class ProcessorTests {

    @Test
    public void ensureProcessExecutesSuccessfully() {
        assume().about(javaSource())
            .that(JavaFileObjects.forResource("Person.java"))
            .processedWith(new org.inferred.freebuilder.processor.Processor(), new Processor())
            .compilesWithoutError();
    }
}
