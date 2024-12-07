/*
 * Based on original work Copyright 2014-2016 Squarespace, Inc.
 * Modified by CMS DPC
 * 
 * Licensed under the Apache License, Version 2.0
 */


package com.squarespace.jersey2.guice;

import com.google.inject.Guice;
import com.google.inject.Inject;
import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.InjectionResolver;

/**
 * The {@link GuiceInjectionResolver} delegates all {@link Guice}'s {@link com.google.inject.Inject}
 * binding annotations to JSR-330's {@link jakarta.inject.Inject}.
 *
 * @see GuiceThreeThirtyResolver
 */
public class GuiceInjectionResolver extends AbstractInjectionResolver<com.google.inject.Inject> {

    /**
     * The name of the {@link InjectionResolver} for {@link Guice}'s own
     * {@link com.google.inject.Inject} binding annotation. It's just a
     * delegate to {@link InjectionResolver#SYSTEM_RESOLVER_NAME} and doesn't
     * do anything special.
     */
    public static final String GUICE_RESOLVER_NAME = "GuiceInjectionResolver";

    private final ActiveDescriptor<? extends InjectionResolver<?>> descriptor;

    @Inject
    public GuiceInjectionResolver(ActiveDescriptor<? extends InjectionResolver<?>> descriptor) {
        this.descriptor = descriptor;
    }

    @Override
    public Object resolve(Injectee injectee, ServiceHandle<?> root) {
        InjectionResolver<?> resolver = descriptor.create(root);
        return resolver.resolve(injectee, root);
    }
}
