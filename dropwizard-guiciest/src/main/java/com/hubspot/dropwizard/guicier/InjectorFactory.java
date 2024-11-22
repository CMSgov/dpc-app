/*
 * Based on original work Copyright 2019 Hubspot, Inc.
 * Modified by CMS DPC
 * 
 * Licensed under the Apache License, Version 2.0
 */
package com.hubspot.dropwizard.guicier;

import com.google.inject.*;
import com.google.inject.util.Modules;

public interface InjectorFactory {
    Injector create(Stage stage, com.google.inject.Module module);

    default Injector create(Stage stage, Iterable<? extends com.google.inject.Module> modules) {
        return create(stage, Modules.combine(modules));
    }
}
