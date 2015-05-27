package org.joeyb.freemapper.processor;

import static com.google.common.truth.TruthJUnit.assume;
import static org.joeyb.freemapper.processor.StringUtils.camelCaseToSnakeCase;

import org.junit.Test;

public class StringUtilsTests {

    @Test
    public void ensureCamelCaseToSnakeCaseWorks() {
        assume().that(camelCaseToSnakeCase("TestField")).isEqualTo("test_field");
        assume().that(camelCaseToSnakeCase("TestField2")).isEqualTo("test_field2");
        assume().that(camelCaseToSnakeCase("TestALongerField")).isEqualTo("test_a_longer_field");
        assume().that(camelCaseToSnakeCase("Test")).isEqualTo("test");
    }
}
