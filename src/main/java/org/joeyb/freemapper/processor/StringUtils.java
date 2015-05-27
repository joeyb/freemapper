package org.joeyb.freemapper.processor;

import java.util.regex.Pattern;

class StringUtils {

    private static final Pattern CAMEL_CASE_TO_SNAKE_CASE_PATTERN =
        Pattern.compile("((?<=[a-z0-9])[A-Z]|(?!^)[A-Z](?=[a-z]))");

    public static String camelCaseToSnakeCase(String camelCase) {
        return CAMEL_CASE_TO_SNAKE_CASE_PATTERN.matcher(camelCase)
            .replaceAll("_$1")
            .toLowerCase();
    }

    private StringUtils() { }
}
