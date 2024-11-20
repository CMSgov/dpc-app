/*
 * Based on original work Copyright 2019 Hubspot, Inc.
 * Modified by CMS DPC
 * 
 * Licensed under the Apache License, Version 2.0
 */
package com.hubspot.dropwizard.guicier;

import com.codahale.metrics.health.HealthCheck;
import com.google.inject.*;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.ProvisionListener;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.lifecycle.ServerLifecycleListener;
import io.dropwizard.servlets.tasks.Task;
import org.glassfish.jersey.server.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.ext.Provider;
import java.lang.reflect.Type;
import org.glassfish.jersey.server.ResourceConfig;

public class DropwizardModule extends AbstractModule {
    private static final Logger LOG = LoggerFactory.getLogger(DropwizardModule.class);
    private static final boolean TRACE = LOG.isTraceEnabled();

    private final Environment environment;

    public DropwizardModule(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void configure() {
        bindListener(Matchers.any(), new ProvisionListener() {
            @Override
            public <T> void onProvision(ProvisionInvocation<T> provision) {
                Object obj = provision.provision();

                if (obj instanceof Managed) {
                    handle((Managed) obj);
                }

                if (obj instanceof Task) {
                    handle((Task) obj);
                }

                if (obj instanceof HealthCheck) {
                    handle((HealthCheck) obj);
                }

                if (obj instanceof ServerLifecycleListener) {
                    handle((ServerLifecycleListener) obj);
                }
            }
        });
    }

    public void register(Injector injector) {
        registerResourcesAndProviders(environment.jersey().getResourceConfig(), injector);
    }

    private void handle(Managed managed) {
        environment.lifecycle().manage(managed);
        if(TRACE) LOG.trace("Added guice injected managed Object: {}", managed.getClass().getName());
    }

    private void handle(Task task) {
        environment.admin().addTask(task);
        if(TRACE) LOG.trace("Added guice injected Task: {}", task.getClass().getName());
    }

    private void handle(HealthCheck healthcheck) {
        environment.healthChecks().register(healthcheck.getClass().getSimpleName(), healthcheck);
        if(TRACE) LOG.trace("Added guice injected health check: {}", healthcheck.getClass().getName());
    }

    private void handle(ServerLifecycleListener serverLifecycleListener) {
        environment.lifecycle().addServerLifecycleListener(serverLifecycleListener);
        if(TRACE) LOG.trace("Added guice injected server lifecycle listener: {}", serverLifecycleListener.getClass().getName());
    }

    private void registerResourcesAndProviders(ResourceConfig config, Injector injector) {
        while (injector != null) {
            for (Key<?> key : injector.getBindings().keySet()) {
                Type type = key.getTypeLiteral().getType();
                if (type instanceof Class<?>) {
                    Class<?> c = (Class<?>) type;
                    if (isProviderClass(c)) {

                        if(TRACE) LOG.trace("Registering {} as a provider class", c.getName());
                        config.register(c);
                    } else if (isResourceClass(c)) {
                        // Jersey rejects resources that it doesn't think are acceptable
                        // Including abstract classes and interfaces, even if there is a valid Guice binding.
                        if (Resource.isAcceptable(c)) {
                            if(TRACE) LOG.trace("Registering {} as a root resource class", c.getName());
                            config.register(c);
                        } else {
                            if(TRACE) LOG.trace("Class {} was not registered as a resource; bind a concrete implementation instead", c.getName());
                        }
                    }

                }
            }
            injector = injector.getParent();
        }
    }

    private static boolean isProviderClass(Class<?> c) {
        return c.isAnnotationPresent(Provider.class);
    }

    private static boolean isResourceClass(Class<?> c) {
        if (c.isAnnotationPresent(Path.class)) {
            return true;
        }

        for (Class<?> i : c.getInterfaces()) {
            if (i.isAnnotationPresent(Path.class)) {
                return true;
            }
        }

        return false;
    }
}
