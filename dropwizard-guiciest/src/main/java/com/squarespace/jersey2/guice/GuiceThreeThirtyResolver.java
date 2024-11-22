/*
 * Based on original work Copyright 2014-2016 Squarespace, Inc.
 * Modified by CMS DPC
 * 
 * Licensed under the Apache License, Version 2.0
 */


package com.squarespace.jersey2.guice;

import com.google.inject.Guice;
import jakarta.inject.Singleton;

import org.glassfish.hk2.api.*;

import jakarta.annotation.Nullable;

import static com.squarespace.jersey2.guice.BindingUtils.isNullable;
import jakarta.inject.Inject;

/**
 * This is a replacement for HK2's ThreeThirtyResolver. It adds  support for JSR-305's
 * {@link Nullable} and {@link Guice}'s own {@link com.google.inject.Inject#optional()}.
 *
 * @see BindingUtils#isNullable(Injectee)
 * @see Nullable
 * @see com.google.inject.Inject#optional()
 */
@Singleton
@Rank(Integer.MAX_VALUE)
@org.jvnet.hk2.annotations.Service
public class GuiceThreeThirtyResolver extends AbstractInjectionResolver<jakarta.inject.Inject> {

    private final ServiceLocator locator;

    @Inject
    public GuiceThreeThirtyResolver(ServiceLocator locator) {
        this.locator = locator;
    }

    @Override
    public Object resolve(Injectee injectee, ServiceHandle<?> root) {
        ActiveDescriptor<?> descriptor = locator.getInjecteeDescriptor(injectee);

        if (descriptor == null) {

            // Is it OK to return null?
            if (isNullable(injectee)) {
                return null;
            }

            throw new MultiException(new UnsatisfiedDependencyException(injectee));
        }

        return locator.getService(descriptor, root, injectee);
    }
}