/*
 * Based on original work Copyright 2014-2016 Squarespace, Inc.
 * Modified by CMS DPC
 * 
 * Licensed under the Apache License, Version 2.0
 */


package com.squarespace.jersey2.guice;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.extension.ServiceLocatorGenerator;

import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Jersey/HK2 uses unfortunately SPIs for custom {@link ServiceLocator}s.
 *
 * @see ServiceLocatorGenerator
 */
public class GuiceServiceLocatorGeneratorStub implements ServiceLocatorGenerator {
    
    private static final Logger LOG = LoggerFactory.getLogger(GuiceServiceLocatorGeneratorStub.class);
    
    private static final AtomicReference<ServiceLocatorGenerator> GENERATOR_REF = new AtomicReference<>();

    static ServiceLocatorGenerator install(ServiceLocatorGenerator generator) {
        LOG.info("I got a generator to install!  Its a " + generator.getClass().getCanonicalName());
        if (generator instanceof GuiceServiceLocatorGeneratorStub) {
            throw new IllegalArgumentException();
        }

        ServiceLocatorGenerator g = GENERATOR_REF.getAndSet(generator);
        LOG.info("OK! I got and set a generator and am returning a " + generator.getClass().getCanonicalName());
        return g;
    }

    static ServiceLocatorGenerator get() {
        return GENERATOR_REF.get();
    }

    @Override
    public ServiceLocator create(String name, ServiceLocator parent) {
        ServiceLocatorGenerator generator = GENERATOR_REF.get();

        if (generator == null) {
            throw new IllegalStateException("It appears there is no ServiceLocatorGenerator installed.");
        }

        ServiceLocator l = generator.create(name, parent);
        LOG.info("I just created a new service locator: " + l.getName());
        return l;
    }
}
