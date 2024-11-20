/*
 * Based on original work Copyright 2014-2016 Squarespace, Inc.
 * Modified by CMS DPC
 * 
 * Licensed under the Apache License, Version 2.0
 */


package com.squarespace.jersey2.guice;

import com.google.inject.*;
import org.glassfish.hk2.utilities.Binder;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

/**
 * A HK2 {@link Binder} for {@link Guice} type(s).
 *
 * @see GuiceBindingDescriptor
 */
class GuiceBinder<T> extends AbstractBinder {

    private final Key<T> key;

    private final Binding<T> binding;

    public GuiceBinder(Key<T> key, Binding<T> binding) {
        this.key = key;
        this.binding = binding;
    }

    @Override
    protected void configure() {
        bind(newDescriptor(key, binding));
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof GuiceBinder<?>)) {
            return false;
        }

        GuiceBinder<?> other = (GuiceBinder<?>) o;
        return key.equals(other.key);
    }

    @Override
    public String toString() {
        return key.toString();
    }

    private static <T> GuiceBindingDescriptor<T> newDescriptor(Key<T> key, Binding<T> binding) {

        TypeLiteral<T> typeLiteral = key.getTypeLiteral();

        Type type = typeLiteral.getType();
        Class<?> clazz = typeLiteral.getRawType();

        Set<Annotation> qualifiers = BindingUtils.getQualifiers(key);

        return new GuiceBindingDescriptor<>(type, clazz, qualifiers, binding);
    }
}
