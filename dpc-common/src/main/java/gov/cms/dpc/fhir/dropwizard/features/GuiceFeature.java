package gov.cms.dpc.fhir.dropwizard.features;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import gov.cms.dpc.fhir.FHIRModule;
import gov.cms.dpc.fhir.configuration.DPCFHIRConfiguration;
import gov.cms.dpc.fhir.validations.dropwizard.FHIRValidationModule;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.ServiceLocatorProvider;
import org.jvnet.hk2.guice.bridge.api.GuiceBridge;
import org.jvnet.hk2.guice.bridge.api.GuiceIntoHK2Bridge;

import javax.annotation.Priority;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

@Priority(1)
public class GuiceFeature implements Feature {

    @Inject
    public GuiceFeature() {
        // Not used
    }
    @Override
    public boolean configure(FeatureContext context) {
        final ServiceLocator locator = ServiceLocatorProvider.getServiceLocator(context);
        GuiceBridge.getGuiceBridge().initializeGuiceBridge(locator);
//        final Injector injector = Guice.createInjector(new FHIRValidationModule(new DPCFHIRConfiguration.FHIRValidationConfiguration()));
        final Injector injector = null;
        final GuiceIntoHK2Bridge bridge = locator.getService(GuiceIntoHK2Bridge.class);
        bridge.bridgeGuiceInjector(injector);
        return true;
    }
}
