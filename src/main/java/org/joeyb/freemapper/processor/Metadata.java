package org.joeyb.freemapper.processor;

import org.inferred.freebuilder.FreeBuilder;

import java.util.List;

import javax.lang.model.element.TypeElement;

@FreeBuilder
public interface Metadata {

    TypeElement getBuilderElement();

    TypeElement getElement();

    String getMapperName();

    String getName();

    String getPackageName();

    List<Property> getProperties();

    class Builder extends Metadata_Builder { }
}
