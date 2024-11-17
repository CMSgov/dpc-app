/*
 * Based on original work Copyright 2019 Hubspot, Inc.
 * Modified by CMS DPC
 * 
 * Licensed under the Apache License, Version 2.0
 */
package com.hubspot.dropwizard.guicier;

import com.google.inject.Provides;
import com.google.inject.servlet.RequestScoped;
import com.squarespace.jersey2.guice.JerseyModule;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.server.ExtendedUriInfo;

import jakarta.servlet.ServletConfig;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Configuration;

/**
 * This supplements the bindings provided in {@link com.squarespace.jersey2.guice.JerseyGuiceModule}.
 */
public class JerseyGuicierModule extends JerseyModule {

    @Override
    protected void configure() {
    }

    @Provides
    public Configuration providesConfiguration(ServiceLocator serviceLocator) {
        return serviceLocator.getService(Configuration.class);
    }

    @Provides
    @RequestScoped
    public ContainerRequestContext providesContainerRequestContext(ServiceLocator serviceLocator) {
        return serviceLocator.getService(ContainerRequestContext.class);
    }

    @Provides
    @RequestScoped
    public ExtendedUriInfo providesExtendedUriInfo(ServiceLocator serviceLocator) {
        return serviceLocator.getService(ExtendedUriInfo.class);
    }

    @Provides
    public ResourceContext providesResourceContext(ServiceLocator serviceLocator) {
        return serviceLocator.getService(ResourceContext.class);
    }

    @Provides
    public ServletConfig providesServletConfig(ServiceLocator serviceLocator) {
        return serviceLocator.getService(ServletConfig.class);
    }
}
