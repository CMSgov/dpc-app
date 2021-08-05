package gov.cms.dpc.fhir.hapi;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.dpc.fhir.DPCResourceType;

import java.util.List;

public class ContextUtils {
    private ContextUtils() {
        // this is a static class
    }

    /**
     * This optimization primes the passed in context with the resource models it needs. Priming a HAPI FhirContext is
     * an expensive operation that only needs to be done once, preferable at service start time. Although the HAPI
     * library is multi-threaded, it locks while a resource model is downloaded. If a context is not primed before use
     * it may lock up its thread and other threads as well.
     *
     * @param context to prime
     * @param resourcesTypes that are used
     */
    public static void prefetchResourceModels(FhirContext context, List<DPCResourceType> resourcesTypes) {
        context.getResourceDefinition("OperationOutcome");
        context.getResourceDefinition("Bundle");
        for(var resourceType: resourcesTypes) {
            context.getResourceDefinition(resourceType.name());
        }
    }
}
