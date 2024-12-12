/*
 * Based on original work Copyright 2019 Hubspot, Inc.
 * Modified by CMS DPC
 * 
 * Licensed under the Apache License, Version 2.0
 */
package com.hubspot.dropwizard.guicier;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.core.setup.Bootstrap;

/**
 * Dropwizard makes it really hard to allow unknown fields in the io.dropwizard.Configuration}
 *
 * @see <a href="https://github.com/dropwizard/dropwizard/blob/ca39aa51a87a596e7b7e7e1309f9c34d9544bf63/dropwizard-configuration/src/main/java/io/dropwizard/configuration/YamlConfigurationFactory.java#L71-L72">Dropwizard Source</a>
 */
public class AllowUnknownFieldsObjectMapper extends ObjectMapper {
    private static final long serialVersionUID = 1L;

    private AllowUnknownFieldsObjectMapper(ObjectMapper source) {
        super(source);
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public static void applyTo(Bootstrap<?> bootstrap) {
        bootstrap.setObjectMapper(new AllowUnknownFieldsObjectMapper(bootstrap.getObjectMapper()));
    }

    @Override
    public ObjectMapper copy() {
        return new AllowUnknownFieldsObjectMapper(this);
    }

    @Override
    public ObjectMapper configure(DeserializationFeature f, boolean state) {
        if (f != DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) {
            return super.configure(f, state);
        } else {
            return this;
        }
    }

    @Override
    public ObjectMapper enable(DeserializationFeature feature) {
        if (feature != DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) {
            return super.enable(feature);
        } else {
            return this;
        }
    }

    @Override
    public ObjectMapper enable(DeserializationFeature first, DeserializationFeature... f) {
        if (first != DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) {
            super.enable(first);
        }

        for (DeserializationFeature feature : f) {
            if (feature != DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) {
                super.enable(feature);
            }
        }

        return this;
    }
}
