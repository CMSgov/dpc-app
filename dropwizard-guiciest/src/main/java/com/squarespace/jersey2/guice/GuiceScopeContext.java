/*
 * Based on original work Copyright 2014-2016 Squarespace, Inc.
 * Modified by CMS DPC
 * 
 * Licensed under the Apache License, Version 2.0
 */


package com.squarespace.jersey2.guice;

import org.glassfish.hk2.api.*;
import org.jvnet.hk2.annotations.Service;

import java.lang.annotation.Annotation;

@Service
class GuiceScopeContext implements Context<GuiceScope> {

    @Override
    public Class<? extends Annotation> getScope() {
        return GuiceScope.class;
    }

    @Override
    public <U> U findOrCreate(ActiveDescriptor<U> descriptor, ServiceHandle<?> root) {
        return descriptor.create(root);
    }

    @Override
    public boolean containsKey(ActiveDescriptor<?> descriptor) {
        return false;
    }

    @Override
    public void destroyOne(ActiveDescriptor<?> descriptor) {
    }

    @Override
    public boolean supportsNullCreation() {
        return false;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public void shutdown() {
    }
}
