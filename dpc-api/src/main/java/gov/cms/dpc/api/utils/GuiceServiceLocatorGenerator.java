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

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.extension.ServiceLocatorGenerator;
import org.jvnet.hk2.external.generator.ServiceLocatorGeneratorImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class GuiceServiceLocatorGenerator implements ServiceLocatorGenerator {
  
    private static final Logger LOG = LoggerFactory.getLogger(GuiceServiceLocatorGenerator.class);
    
    private final ServiceLocatorGenerator generator = new ServiceLocatorGeneratorImpl();

    private final ConcurrentMap<String, ServiceLocator> locators = new ConcurrentHashMap<>();

    private final AtomicReference<ServiceLocatorGenerator> delegateRef = new AtomicReference<>();

    public void delegate(ServiceLocatorGenerator delegate) {
        LOG.info("Soneone asked me to delegate a " + delegate.getClass().getCanonicalName());
        delegateRef.set(delegate);
    }
  
    public void add(ServiceLocator locator) {
        String name = locator.getName();
        LOG.info("OK I got a new SL to add: " + name);
    
        if (locators.putIfAbsent(name, locator) != null) {
            LOG.error("This is a duplicate!");
            throw new IllegalStateException("Duplicate name: " + name);
        }
    }
  
    public Collection<ServiceLocator> locators() {
        LOG.info("Someone wants the set of SLs!");
        return locators.values();
    }

    public void reset() {
        LOG.info("Someone wants me to clear " + locators.size() + " locators!");
        locators.clear();
        LOG.info("And to null out a delegate, is it already null? " + (delegateRef.get() == null));
        delegateRef.set(null);
    }
  
    @Override
    public ServiceLocator create(String name, ServiceLocator parent) {
        // Using remove() to transfer ownership of the ServiceLocator from
        // this object to the caller. Something is really wrong if the caller 
        // uses the same name again!
        LOG.info("Someone wants me to create a locator: " + name + " / " + parent);
        ServiceLocator locator = locators.remove(name);
        LOG.info("Well it was already in the locators map, so I'll return it to them rather than create one!");
        if (locator != null) {
            return locator;
        }
    
        // This is mostly needed for testing.
        LOG.info("Let me see if there is already a delegate generator!");
        ServiceLocatorGenerator delegate = delegateRef.get();
        LOG.info("Was there a delegate? " + (delegate == null ? "No!" : delegate.getClass().getCanonicalName()));
        if (delegate != null) {
            LOG.info("OK I will use this generator to produce a locator!");
            locator = delegate.create(name, parent);
            if (locator != null) {
                LOG.info("Created the locator: " + locator.getName() + " / " + parent);
                return locator;
            }
        }
    
        LOG.info("Now returning a new locator from the generator using " + name + " / " + parent);
        return generator.create(name, parent);
    }
}