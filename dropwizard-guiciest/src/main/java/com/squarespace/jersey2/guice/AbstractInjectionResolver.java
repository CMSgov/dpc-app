/*
 * Based on original work Copyright 2014-2016 Squarespace, Inc.
 * Modified by CMS DPC
 * 
 * Licensed under the Apache License, Version 2.0
 */


package com.squarespace.jersey2.guice;

import org.glassfish.hk2.api.InjectionResolver;

/**
 * An abstract implementation of {@link InjectionResolver}.
 */
abstract class AbstractInjectionResolver<T> implements InjectionResolver<T> {

    @Override
    public boolean isConstructorParameterIndicator() {
        return false;
    }

    @Override
    public boolean isMethodParameterIndicator() {
        return false;
    }
}
