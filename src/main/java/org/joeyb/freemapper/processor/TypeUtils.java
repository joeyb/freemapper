package org.joeyb.freemapper.processor;

import javax.lang.model.type.DeclaredType;

class TypeUtils {

    static boolean erasesToAnyOf(DeclaredType type, Class<?>... possibilities) {
        String erasedType = type.asElement().toString();

        for (Class<?> possibility : possibilities) {
            if (possibility.getName().equals(erasedType)) {
                return true;
            }
        }
        return false;
    }

    private TypeUtils() { }
}
