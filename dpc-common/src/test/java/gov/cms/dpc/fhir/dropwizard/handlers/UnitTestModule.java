/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package gov.cms.dpc.fhir.dropwizard.handlers;

import ca.uhn.fhir.context.FhirContext;
import com.google.inject.AbstractModule;
import com.squarespace.jersey2.guice.GuiceServiceLocatorGeneratorStub;
import com.squarespace.jersey2.guice.GuiceServiceLocator;
import io.dropwizard.core.setup.Environment;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.extension.ServiceLocatorGenerator;

/**
 *
 * @author ceh1
 */
public class UnitTestModule extends AbstractModule {

    private final Environment environment;
    
    public UnitTestModule(Environment environment) {
        this.environment = environment;
    }

    @Override
    protected void configure() {
        // Bind your test dependencies here
        bind(ServiceLocator.class).to(GuiceServiceLocator.class);  // GuiceServiceLocator should be the implementation you're using
        bind(ServiceLocatorGenerator.class).to(GuiceServiceLocatorGeneratorStub.class);
        bind(FHIRHandler.class).toInstance(new FHIRHandler(FhirContext.forDstu3()));
    }
}

