/*
 * Based on original work Copyright 2019 Hubspot, Inc.
 * Modified by CMS DPC
 * 
 * Licensed under the Apache License, Version 2.0
 */
package com.hubspot.dropwizard.guicier;

import com.google.common.collect.ImmutableSet;
import com.squarespace.jersey2.guice.JerseyGuiceModule;
import com.squarespace.jersey2.guice.JerseyGuiceUtils;
import org.glassfish.hk2.internal.ServiceLocatorFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.ConfiguredBundle;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import org.jvnet.hk2.guice.bridge.api.GuiceBridge;
import org.jvnet.hk2.guice.bridge.api.GuiceIntoHK2Bridge;

/**
 * GuiceBundle used to configure guice with a set of modules and establish necessary bindings
 * @author <a href="mailto:henning@schmiedehausen.org">Henning P. Schmiedehausen</a>
 * @param <T> class that extends Configuration
 */
public class GuiceBundle<T extends Configuration> implements ConfiguredBundle<T> {
    private static final Logger LOG = LoggerFactory.getLogger(GuiceBundle.class);

    public static <U extends Configuration> Builder<U> defaultBuilder(final Class<U> configClass) {
        return new Builder<>(configClass);
    }

    private final Class<T> configClass;
    private final ImmutableSet<DropwizardAwareModule<T>> dropwizardAwareModules;
    private final ImmutableSet<com.google.inject.Module> guiceModules;
    private final Stage guiceStage;
    private final boolean allowUnknownFields;
    private final boolean enableGuiceEnforcer;
    private final InjectorFactory injectorFactory;

    private Bootstrap<T> bootstrap;
    private Injector injector;

    private GuiceBundle(final Class<T> configClass,
                        final ImmutableSet<com.google.inject.Module> guiceModules,
                        final ImmutableSet<DropwizardAwareModule<T>> dropwizardAwareModules,
                        final Stage guiceStage,
                        final boolean allowUnknownFields,
                        final boolean enableGuiceEnforcer,
                        final InjectorFactory injectorFactory) {
        this.configClass = configClass;

        this.guiceModules = guiceModules;
        this.dropwizardAwareModules = dropwizardAwareModules;
        this.guiceStage = guiceStage;
        this.allowUnknownFields = allowUnknownFields;
        this.enableGuiceEnforcer = enableGuiceEnforcer;
        this.injectorFactory = injectorFactory;
    }

    @Override
    public void initialize(final Bootstrap<?> bootstrap) {
        LOG.info("Initializing GuiceBundle...");

        this.bootstrap = (Bootstrap<T>) bootstrap;
        if (allowUnknownFields) {
            AllowUnknownFieldsObjectMapper.applyTo(bootstrap);
        }
    }

    @Override
    public void run(final T configuration, final Environment environment) throws Exception {
        LOG.info("Running GuiceBundle...");


        for (DropwizardAwareModule<T> dropwizardAwareModule : dropwizardAwareModules) {
            dropwizardAwareModule.setBootstrap(bootstrap);
            dropwizardAwareModule.setConfiguration(configuration);
            dropwizardAwareModule.setEnvironment(environment);

            LOG.info("Prepped DropwizardAwareModule " + dropwizardAwareModule);
        }

        final DropwizardModule dropwizardModule = new DropwizardModule(environment);

        // We assume that the next service locator will be the main application one
        final String serviceLocatorName = getNextServiceLocatorName();
        LOG.info("Here is the service locator name: " + serviceLocatorName);

        JerseyGuiceModule jgm = new JerseyGuiceModule(serviceLocatorName);
        
        ImmutableSet.Builder<com.google.inject.Module> modulesBuilder =
                ImmutableSet.<com.google.inject.Module>builder()
                        .addAll(guiceModules)
                        .addAll(dropwizardAwareModules)
//                        .add(new SingletonModule())
                        .add(new ServletModule())
                        .add(dropwizardModule)
                        .add(jgm)
                        .add(new JerseyGuicierModule())
                        .add(binder -> {
                            binder.bind(Environment.class).toInstance(environment);
                            binder.bind(configClass).toInstance(configuration);
                        });
        LOG.info("I set up the modules builder!");
        
        if (enableGuiceEnforcer) {
            modulesBuilder.add(new GuiceEnforcerModule());
            LOG.info("Guice enforcer added!");
        }

        this.injector = injectorFactory.create(guiceStage, modulesBuilder.build());

        JerseyGuiceUtils.install(injector);
        LOG.info("Installed the injector!");

        GuiceBridge.getGuiceBridge().initializeGuiceBridge(jgm.getLocator());
        GuiceIntoHK2Bridge guiceBridge = jgm.getLocator().getService(GuiceIntoHK2Bridge.class);
        guiceBridge.bridgeGuiceInjector(injector);
        LOG.info("Setup Guice-HK2 Bridges!");
        
        dropwizardModule.register(injector);

        environment.servlets().addServletListeners(new GuiceServletContextListener() {

            @Override
            protected Injector getInjector() {
                return injector;
            }
        });
    }

    public Injector getInjector() {
        return checkNotNull(injector, "injector has not been initialized yet");
    }

    public static class GuiceEnforcerModule implements com.google.inject.Module {
        @Override
        public void configure(final Binder binder) {
            binder.disableCircularProxies();
            binder.requireExplicitBindings();
            binder.requireExactBindingAnnotations();
            binder.requireAtInjectOnConstructors();
        }
    }

    public static class Builder<U extends Configuration> {
        private final Class<U> configClass;
        private final ImmutableSet.Builder<com.google.inject.Module> guiceModules = ImmutableSet.builder();
        private final ImmutableSet.Builder<DropwizardAwareModule<U>> dropwizardAwareModules = ImmutableSet.builder();
        private Stage guiceStage = Stage.PRODUCTION;
        private boolean allowUnknownFields = true;
        private boolean enableGuiceEnforcer = true;
        private InjectorFactory injectorFactory = Guice::createInjector;

        private Builder(final Class<U> configClass) {
            this.configClass = configClass;
        }

        public final Builder<U> stage(final Stage guiceStage) {
            checkNotNull(guiceStage, "guiceStage is null");
            this.guiceStage = guiceStage;
            return this;
        }

        public final Builder<U> allowUnknownFields(final boolean allowUnknownFields) {
            this.allowUnknownFields = allowUnknownFields;
            return this;
        }

        public final Builder<U> enableGuiceEnforcer(final boolean enableGuiceEnforcer) {
            this.enableGuiceEnforcer = enableGuiceEnforcer;
            return this;
        }

        public final Builder<U> modules(final com.google.inject.Module... modules) {
            return modules(Arrays.asList(modules));
        }

        @SuppressWarnings("unchecked")
        public final Builder<U> modules(final Iterable<? extends com.google.inject.Module> modules) {
            for (com.google.inject.Module module : modules) {
                if (module instanceof DropwizardAwareModule<?>) {
                    dropwizardAwareModules.add((DropwizardAwareModule<U>) module);
                } else {
                    guiceModules.add(module);
                }
            }
            return this;
        }

        public final Builder<U> injectorFactory(final InjectorFactory injectorFactory) {
            this.injectorFactory = injectorFactory;
            return this;
        }

        public final GuiceBundle<U> build() {
            return new GuiceBundle<>(configClass,
                    guiceModules.build(),
                    dropwizardAwareModules.build(),
                    guiceStage,
                    allowUnknownFields,
                    enableGuiceEnforcer,
                    injectorFactory);
        }
    }

    private static String getNextServiceLocatorName() {
        Class<ServiceLocatorFactoryImpl> factoryClass = ServiceLocatorFactoryImpl.class;
        try {
            Field nameCountField = factoryClass.getDeclaredField("name_count");
            nameCountField.setAccessible(true);
            int count = (int) nameCountField.get(null);

            Field namePrefixField = factoryClass.getDeclaredField("GENERATED_NAME_PREFIX");
            namePrefixField.setAccessible(true);
            String prefix = (String) namePrefixField.get(null);

            return prefix + count;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
