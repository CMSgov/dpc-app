/*
 * Based on original work Copyright 2014-2016 Squarespace, Inc.
 * Modified by CMS DPC
 * 
 * Licensed under the Apache License, Version 2.0
 */


package com.squarespace.jersey2.guice;

import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.servlet.RequestScoped;
import com.hubspot.dropwizard.guicier.ConcreteJerseyClassAnalyzer;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import org.glassfish.hk2.api.ServiceLocator;

import jakarta.ws.rs.ext.Providers;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.ClassAnalyzer;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.jersey.inject.hk2.Hk2RequestScope;
import org.glassfish.jersey.process.internal.RequestScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JerseyGuiceModule extends JerseyModule {

    private static final Logger LOG = LoggerFactory.getLogger(JerseyGuiceModule.class);
    private static final boolean TRACE = LOG.isTraceEnabled();
    
    private final ServiceLocator locator;

    public JerseyGuiceModule(String name) {
        this(JerseyGuiceUtils.newServiceLocator(name));
        if(TRACE) LOG.trace("Creating JGM with service locator name: " + name);
    }

    public JerseyGuiceModule(ServiceLocator locator) {
        if(TRACE) LOG.trace("Creating JGM with service locator: " + locator.getName());
        this.locator = locator;
    }

    @Override
    protected void configure() {
        if(TRACE) LOG.trace("I am configuring the JerseyGuiceModule!");
       
        bind(ServiceLocator.class).toInstance(locator);
        if(TRACE) LOG.trace("Bound Service locator.");

        Hk2RequestScope requestScope = new Hk2RequestScope();
        
        // Create a provider for the specific ActiveDescriptor needed
        bind(new TypeLiteral<ActiveDescriptor<? extends InjectionResolver<?>>>() {})
            .toProvider(new com.google.inject.Provider<ActiveDescriptor<? extends InjectionResolver<?>>>() {
                @Inject
                private Provider<ServiceLocator> locatorProvider;

                @Override
                public ActiveDescriptor<? extends InjectionResolver<?>> get() {
                    ServiceLocator locator = locatorProvider.get();
                    @SuppressWarnings("unchecked")
                    ActiveDescriptor<? extends InjectionResolver<?>> descriptor = 
                        (ActiveDescriptor<? extends InjectionResolver<?>>) locator.getBestDescriptor(
                            BuilderHelper.createContractFilter(InjectionResolver.class.getName())
                        );
                    return descriptor;
                }
            }).in(Scopes.SINGLETON);


        bind(new TypeLiteral<InjectionResolver<com.google.inject.Inject>>() {}).to(GuiceInjectionResolver.class).in(Scopes.SINGLETON);
        bind(InjectionResolver.class).to(GuiceInjectionResolver.class).in(Scopes.SINGLETON);
        if(TRACE) LOG.trace("Bound InjectionResolver");

        bind(ClassAnalyzer.class).to(ConcreteJerseyClassAnalyzer.class).in(Scopes.SINGLETON);
        if(TRACE) LOG.trace("Bound ClassAnalyzer");
        
        bind(Hk2RequestScope.class).toInstance(requestScope);
        bind(RequestScope.class).toInstance(requestScope);
        if(TRACE) LOG.trace("Bound RequestScope");

        Provider<ServiceLocator> provider = getProvider(ServiceLocator.class);

        bind(Application.class)
                .toProvider(new JerseyProvider<>(provider, Application.class));
        if(TRACE) LOG.trace("Bound Application to JerseyProvider.");

        bind(Providers.class)
                .toProvider(new JerseyProvider<>(provider, Providers.class));
        if(TRACE) LOG.trace("Bound Providers to JerseyProvider.");

        bind(UriInfo.class)
                .toProvider(new JerseyProvider<>(provider, UriInfo.class))
                .in(RequestScoped.class);
        if(TRACE) LOG.trace("Bound UriInfo to JerseyProvider with RequestScope.");

        bind(HttpHeaders.class)
                .toProvider(new JerseyProvider<>(provider, HttpHeaders.class))
                .in(RequestScoped.class);
        if(TRACE) LOG.trace("Bound HttpHeaders to JerseyProvider with RequestScope.");

        bind(SecurityContext.class)
                .toProvider(new JerseyProvider<>(provider, SecurityContext.class))
                .in(RequestScoped.class);
        if(TRACE) LOG.trace("Bound SecurityContext to JerseyProvider with RequestScope.");

        bind(Request.class)
                .toProvider(new JerseyProvider<>(provider, Request.class))
                .in(RequestScoped.class);
        if(TRACE) LOG.trace("Bound Request to JerseyProvider with RequestScope.");

        if(TRACE) LOG.trace("JerseyGuiceModule configuration complete!");
    }
    
   // Explicitly provide an ActiveDescriptor<GuiceInjectionResolver> binding
    @Provides
    @Singleton
    ActiveDescriptor<GuiceInjectionResolver> provideActiveDescriptor(Injector injector) {
        // Retrieve the binding for GuiceInjectionResolver from the injector
        var guiceBinding = injector.getBinding(GuiceInjectionResolver.class);

        // Create and return an ActiveDescriptor for GuiceInjectionResolver using the binding
        return new GuiceBindingDescriptor<>(
            GuiceInjectionResolver.class,            // Class type
            GuiceInjectionResolver.class,            // Implementation class
            Collections.emptySet(),                  // No qualifiers
            guiceBinding                             // The binding retrieved from the injector
        );
    }

    @Provides
    @Singleton
    @SuppressWarnings("rawtypes")
    List<InjectionResolver> provideInjectionResolvers(ActiveDescriptor<GuiceInjectionResolver> descriptor) {
        // Return a list containing GuiceInjectionResolver initialized with the descriptor
        return Arrays.asList(new GuiceInjectionResolver(descriptor));
    }

    public ServiceLocator getLocator() {
        return locator;
    }    

    private static class JerseyProvider<T> implements Provider<T> {

        private Provider<ServiceLocator> provider;

        private final Class<T> type;

        public JerseyProvider(Provider<ServiceLocator> provider, Class<T> type) {
            this.provider = provider;
            this.type = type;
        }

        @Override
        public T get() {
            ServiceLocator locator = provider.get();
            return locator.getService(type);
        }
    }
}
