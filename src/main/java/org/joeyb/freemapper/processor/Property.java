package org.joeyb.freemapper.processor;

import org.inferred.freebuilder.FreeBuilder;

import javax.lang.model.type.TypeMirror;

@FreeBuilder
public interface Property {

    String getField();

    String getName();

    String getGetterName();

    TypeMirror getType();

    class Builder extends Property_Builder { }
}
