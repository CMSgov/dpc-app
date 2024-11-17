/*
 * Based on original work Copyright 2014-2016 Squarespace, Inc.
 * Modified by CMS DPC
 * 
 * Licensed under the Apache License, Version 2.0
 */


package com.squarespace.jersey2.guice;

import org.glassfish.hk2.api.Unproxiable;

import jakarta.inject.Scope;
import java.lang.annotation.*;

@Scope
@Unproxiable
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface GuiceScope {
}
