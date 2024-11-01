/*
 * Copyright 2014-2016 Squarespace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//package com.squarespace.jersey2.guice;

package gov.cms.dpc.api.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicInteger;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.hk2.extension.ServiceLocatorGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An utility class to bootstrap HK2's {@link ServiceLocator}s and {@link Guice}'s {@link Injector}s.
 * 
 * @see ServiceLocator
 * @see Injector
 */
public class JerseyGuiceUtils {
  
  private static final Logger LOG = LoggerFactory.getLogger(JerseyGuiceUtils.class);
  
  private static final String MODIFIERS_FIELD = "modifiers";
  
  private static final String PREFIX = "GuiceServiceLocator-";
  
  private static final AtomicInteger NTH = new AtomicInteger();

  private static final AtomicBoolean SPI_CHECKED = new AtomicBoolean(false);  
  private static final AtomicBoolean SPI_PRESENT = new AtomicBoolean(false);  
  
  private JerseyGuiceUtils() {}
  
  /**
   * 
   */
  public static void reset() {
    GuiceServiceLocatorGenerator generator = getOrCreateGuiceServiceLocatorGenerator();
    LOG.info("Did I get or create a GSLG? " + (generator != null));
    generator.reset();
  }
  
  private static synchronized GuiceServiceLocatorGenerator getOrCreateGuiceServiceLocatorGenerator() {
    // Use SPI
    if (isProviderPresent()) {
        LOG.info("Yes a provider is present!");
      GuiceServiceLocatorGenerator generator = (GuiceServiceLocatorGenerator)GuiceServiceLocatorGeneratorStub.get();
      LOG.info("OK I just created a GSLG Stub! " + (generator != null));

      if (generator == null) {
          LOG.info("OK its still null, let's set it up!");
        generator = new GuiceServiceLocatorGenerator();
          LOG.info("OK I just REALLY created a GSLG Stub! " + generator.getClass().getCanonicalName());
        
        GuiceServiceLocatorGeneratorStub.install(generator);
      }
      return generator;
    }
    
    // Use Reflection
    GuiceServiceLocatorFactory factory = getOrCreateFactory();
    if (factory != null) {
      GuiceServiceLocatorGenerator generator = (GuiceServiceLocatorGenerator)factory.get();
      if (generator == null) {
        generator = new GuiceServiceLocatorGenerator();
        factory.install(generator);
      }
      
      return generator;
    }
    
    throw new IllegalStateException();
  }
  
  /**
   * Returns {@code true} if jersey2-guice SPI is present.
   */
  private static synchronized boolean isProviderPresent() {
    
    if (SPI_CHECKED.compareAndSet(false, true)) {

        LOG.info("OK no one has checked on SPI before!");
        
      ServiceLocatorGenerator generator = lookupSPI();
      LOG.info("OK I got back a generator: " + generator.getClass().getCanonicalName());
      
      if (generator instanceof GuiceServiceLocatorGeneratorStub) {
          SPI_PRESENT.set(true);
      }
      
      if (SPI_PRESENT.get()) {
        LOG.warn("It appears jersey2-guice-spi is either not present or in conflict with some other Jar: {}", generator);
      }
    }
    
    return SPI_PRESENT.get();
  }
  
  private static ServiceLocatorGenerator lookupSPI() {
    return AccessController.doPrivileged(new PrivilegedAction<ServiceLocatorGenerator>() {
      @Override
      public ServiceLocatorGenerator run() {
        try {
          ClassLoader classLoader = JerseyGuiceUtils.class.getClassLoader();
          ServiceLoader<ServiceLocatorGenerator> providers 
              = ServiceLoader.load(ServiceLocatorGenerator.class, classLoader);
          
          for(ServiceLocatorGenerator provider : providers)
              LOG.info("Found a SLG: " + provider);
          
          return providers.findFirst().orElse(null);
        } catch (Throwable th) {
            LOG.error("ERROR: " + th);
        }
        
        return null;
      }
    });
  }
  
  /**
   * @see ServiceLocatorFactory
   */
  private static synchronized GuiceServiceLocatorFactory getOrCreateFactory() {
    ServiceLocatorFactory factory = ServiceLocatorFactory.getInstance();
    if (factory instanceof GuiceServiceLocatorFactory) {
      return (GuiceServiceLocatorFactory)factory;
    }
    
    if (LOG.isInfoEnabled()) {
      LOG.info("Attempting to install (using relfection) a Guice-aware ServiceLocatorFactory...");
    }
    
    try {
      GuiceServiceLocatorFactory guiceServiceLocatorFactory = new GuiceServiceLocatorFactory(factory);
      LOG.info("Hey I just created a GSLF!");
      
      Class<?> clazz = ServiceLocatorFactory.class;
      Field field = clazz.getDeclaredField("INSTANCE");
      
      set(field, null, guiceServiceLocatorFactory);
      
      return guiceServiceLocatorFactory;
    } catch (Exception err) {
        err.printStackTrace();
        LOG.error("Exception", err);
    }
    
    return null;
  }

  private static void set(Field field, Object instance, Object value) throws  IllegalAccessException, NoSuchFieldException, SecurityException {
    field.setAccessible(true);
    
    int modifiers = field.getModifiers();
    if (Modifier.isFinal(modifiers)) {
      setModifiers(field, modifiers & ~Modifier.FINAL);
      try {
        field.set(instance, value);
      } finally {
        setModifiers(field, modifiers | Modifier.FINAL);
      }
    } else {
      field.set(instance, value);
    }
  }
  
  private static void setModifiers(Field dst, int modifiers) throws IllegalAccessException, NoSuchFieldException, SecurityException {
    Field field = Field.class.getDeclaredField(MODIFIERS_FIELD);
    field.setAccessible(true);
    field.setInt(dst, modifiers);
  }}