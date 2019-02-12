package gov.cms.dpc.web.features;

import gov.cms.dpc.web.core.annotations.FHIR;
import gov.cms.dpc.web.filters.FHIRRequestFilter;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;

public class FHIRRequestFeature implements DynamicFeature {

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        if (resourceInfo.getResourceMethod().getAnnotation(FHIR.class) != null || resourceInfo.getResourceClass().getAnnotation(FHIR.class) != null) {
            context.register(FHIRRequestFilter.class);
        }
    }
}
