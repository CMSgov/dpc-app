package gov.cms.dpc.web.features;

import gov.cms.dpc.web.core.annotations.FHIR;
import gov.cms.dpc.web.filters.FHIRRequestFilter;

import javax.inject.Inject;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

@Provider
public class FHIRRequestFeature implements DynamicFeature {

    @Inject
    public FHIRRequestFeature() {
//        Not used
    }

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        if (resourceInfo.getResourceMethod().getAnnotation(FHIR.class) != null || resourceInfo.getResourceClass().getAnnotation(FHIR.class) != null) {
            context.register(FHIRRequestFilter.class);
        }
    }
}
