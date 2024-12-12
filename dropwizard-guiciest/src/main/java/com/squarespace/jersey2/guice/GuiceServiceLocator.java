/*
 * Based on original work Copyright 2014-2016 Squarespace, Inc.
 * Modified by CMS DPC
 * 
 * Licensed under the Apache License, Version 2.0
 */


package com.squarespace.jersey2.guice;

import com.google.inject.Inject;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.internal.ServiceLocatorImpl;

/**
 * An extension of {@link ServiceLocatorImpl} that exist primarily for type
 * information and completeness in respect to the other classes.
 */
public class GuiceServiceLocator extends ServiceLocatorImpl {

    // No-argument constructor for Guice
    @Inject
    public GuiceServiceLocator() {
        super("defaultName", null);  // Provide default values or initialize later
    }
    
    public GuiceServiceLocator(String name, ServiceLocator parent) {
        super(name, (ServiceLocatorImpl) parent);
    }
}
