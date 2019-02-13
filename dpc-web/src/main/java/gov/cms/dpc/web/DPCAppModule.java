package gov.cms.dpc.web;

import ca.uhn.fhir.context.FhirContext;
import com.google.inject.Binder;
import com.google.inject.Provides;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import gov.cms.dpc.aggregation.AggregationEngine;
import gov.cms.dpc.web.resources.TestResource;
import gov.cms.dpc.web.resources.v1.BaseResource;
import gov.cms.dpc.web.resources.v1.GroupResource;
import gov.cms.dpc.web.resources.v1.JobResource;

public class DPCAppModule extends DropwizardAwareModule<DPWebConfiguration> {

    private final FhirContext context;

    DPCAppModule() {
        this.context = FhirContext.forR4();
    }

    @Override
    public void configure(Binder binder) {

        // Request/Response handlers
        binder.bind(FHIRHandler.class);

        // This will eventually go away.
        binder.bind(AggregationEngine.class);
        binder.bind(Aggregation.class).asEagerSingleton();
        binder.bind(TestResource.class);
        // V1 Resources
        binder.bind(BaseResource.class);
        binder.bind(GroupResource.class);
        binder.bind(JobResource.class);
    }

    @Provides
    FhirContext getFHIRContext() {
        return this.context;
    }
}
