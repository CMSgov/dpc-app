/*
 * Based on original work Copyright 2014-2016 Squarespace, Inc.
 * Modified by CMS DPC
 * 
 * Licensed under the Apache License, Version 2.0
 */

package com.squarespace.jersey2.guice;

import com.google.inject.AbstractModule;
import org.glassfish.hk2.api.ServiceLocator;

/**
 * Bindings declared in {@link JerseyModule}s are excluded from
 * being exposed to HK2's {@link ServiceLocator}.
 */
public abstract class JerseyModule extends AbstractModule {

}
