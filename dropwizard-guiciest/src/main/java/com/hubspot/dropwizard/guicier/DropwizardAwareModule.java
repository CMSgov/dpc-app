/*
 * Based on original work Copyright 2019 Hubspot, Inc.
 * Modified by CMS DPC
 * 
 * Licensed under the Apache License, Version 2.0
 */
package com.hubspot.dropwizard.guicier;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import com.google.inject.AbstractModule;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;

public abstract class DropwizardAwareModule<C extends Configuration> extends AbstractModule implements com.google.inject.Module {
    private volatile Bootstrap<C> bootstrap;
    private volatile C configuration;
    private volatile Environment environment;

    protected Bootstrap<C> getBootstrap() {
        return checkNotNull(this.bootstrap, "bootstrap was not set!");
    }

    protected C getConfiguration() {
        return checkNotNull(this.configuration, "configuration was not set!");
    }

    protected Environment getEnvironment() {
        return checkNotNull(this.environment, "environment was not set!");
    }

    public void setBootstrap(Bootstrap<C> bootstrap) {
        checkState(this.bootstrap == null, "bootstrap was already set!");
        this.bootstrap = checkNotNull(bootstrap, "bootstrap is null");
    }

    public void setConfiguration(C configuration) {
        checkState(this.configuration == null, "configuration was already set!");
        this.configuration = checkNotNull(configuration, "configuration is null");
    }

    public void setEnvironment(Environment environment) {
        checkState(this.environment == null, "environment was already set!");
        this.environment = checkNotNull(environment, "environment is null");
    }
    
    protected Environment environment() {
        return environment;
    }

    /**
     * returns the application configuration for a dropwizard aware module
     * 
     * @return C application configuration
     */
    protected C configuration() {
        return configuration;
    }
    
}
