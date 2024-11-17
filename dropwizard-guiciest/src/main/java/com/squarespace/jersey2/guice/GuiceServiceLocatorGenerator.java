/*
 * Based on original work Copyright 2014-2016 Squarespace, Inc.
 * Modified by CMS DPC
 * 
 * Licensed under the Apache License, Version 2.0
 */


package com.squarespace.jersey2.guice;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.extension.ServiceLocatorGenerator;
import org.jvnet.hk2.external.generator.ServiceLocatorGeneratorImpl;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class GuiceServiceLocatorGenerator implements ServiceLocatorGenerator {
    
    private static final Logger LOG = LoggerFactory.getLogger(GuiceServiceLocatorGenerator.class);

    private final ServiceLocatorGenerator generator = new ServiceLocatorGeneratorImpl();

    private final ConcurrentMap<String, ServiceLocator> locators = new ConcurrentHashMap<>();

    private final AtomicReference<ServiceLocatorGenerator> delegateRef = new AtomicReference<>();

    public void delegate(ServiceLocatorGenerator delegate) {
        LOG.info("I am setting a delegate: " + delegate.hashCode());
        delegateRef.set(delegate);
    }

    public void add(ServiceLocator locator) {
        String name = locator.getName();

        if (locators.putIfAbsent(name, locator) != null) {
            throw new IllegalStateException("Duplicate name: " + name);
        }
    }

    public Collection<ServiceLocator> locators() {
        return locators.values();
    }

    public void reset() {
        locators.clear();
        delegateRef.set(null);
    }

    @Override
    public ServiceLocator create(String name, ServiceLocator parent) {
        // Using remove() to transfer ownership of the ServiceLocator from
        // this object to the caller. Something is really wrong if the caller
        // uses the same name again!
        ServiceLocator locator = locators.remove(name);
        if (locator != null) {
            LOG.info("Someone wants to create a locator, I'll just return " + locator.getName());
            return locator;
        }

        // This is mostly needed for testing.
        ServiceLocatorGenerator delegate = delegateRef.get();
        if (delegate != null) {
            locator = delegate.create(name, parent);
            if (locator != null) {
                LOG.info("Created a locator from delegate: " + locator.getName());
                return locator;
            }
        }

        return generator.create(name, parent);
    }
}