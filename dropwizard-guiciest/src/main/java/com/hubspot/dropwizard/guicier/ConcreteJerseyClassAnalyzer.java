/*
 * Based on original work Copyright 2019 Hubspot, Inc.
 * Modified by CMS DPC
 * 
 * Licensed under the Apache License, Version 2.0
 */
package com.hubspot.dropwizard.guicier;

import org.glassfish.hk2.api.ClassAnalyzer;
import org.glassfish.hk2.api.InjectionResolver;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConcreteJerseyClassAnalyzer implements ClassAnalyzer {

    @SuppressWarnings("rawtypes")
    private final Provider<List<InjectionResolver>> injectionResolvers;

    // Constructor with injection for ServiceLocator and List of InjectionResolvers
    @Inject
    @SuppressWarnings("rawtypes")
    public ConcreteJerseyClassAnalyzer(Provider<List<InjectionResolver>> injectionResolvers) {
        this.injectionResolvers = injectionResolvers;
    }

    /**
     * Get the constructor to be used when constructing this service.
     */
    @Override
    public <T> Constructor<T> getConstructor(Class<T> clazz) {
        try {
            return clazz.getDeclaredConstructor();  // Retrieve the default constructor
        } catch (NoSuchMethodException e) {
            return null;  // Return null if no constructor is found
        }
    }

    /**
     * Get the set of fields to be used when initializing this service.
     */
    @Override
    public <T> Set<Field> getFields(Class<T> clazz) {
        Set<Field> fields = new HashSet<>();
        for (Field field : clazz.getDeclaredFields()) {
            // You can add custom checks if you need to select specific fields for initialization
            fields.add(field);
        }
        return fields;
    }

    /**
     * Get the set of initializer methods to be used when initializing this service.
     */
    @Override
    public <T> Set<Method> getInitializerMethods(Class<T> clazz) {
        Set<Method> methods = new HashSet<>();
        for (Method method : clazz.getDeclaredMethods()) {
            // Add methods that should be used as initializers
            // For example, look for methods with specific annotations
            if (method.isAnnotationPresent(PostConstruct.class)) {
                methods.add(method);  // Add PostConstruct annotated methods
            }
        }
        return methods;
    }

    /**
     * Get the postConstruct method of the class.
     */
    @Override
    public <T> Method getPostConstructMethod(Class<T> clazz) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(PostConstruct.class)) {
                return method;  // Return the method annotated with @PostConstruct
            }
        }
        return null;  // Return null if no @PostConstruct method is found
    }

    /**
     * Get the preDestroy method of the class.
     */
    @Override
    public <T> Method getPreDestroyMethod(Class<T> clazz) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(PreDestroy.class)) {
                return method;  // Return the method annotated with @PreDestroy
            }
        }
        return null;  // Return null if no @PreDestroy method is found
    }

    // Optional: Additional method to expose the injected list of InjectionResolvers, if needed
    @SuppressWarnings("rawtypes")
    public List<InjectionResolver> getInjectionResolvers() {
        return injectionResolvers.get();
    }
}
