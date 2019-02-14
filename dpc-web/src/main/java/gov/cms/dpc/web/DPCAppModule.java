package gov.cms.dpc.web;

import com.google.inject.Binder;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import gov.cms.dpc.aggregation.AggregationEngine;
import gov.cms.dpc.web.resources.TestResource;
import gov.cms.dpc.web.resources.v1.BaseResource;
import gov.cms.dpc.web.resources.v1.GroupResource;
import gov.cms.dpc.web.resources.v1.JobResource;

public class DPCAppModule extends DropwizardAwareModule<DPWebConfiguration> {

    DPCAppModule() {
        // Not used
    }

    @Override
    public void configure(Binder binder) {

        // This will eventually go away.
        binder.bind(AggregationEngine.class);
        binder.bind(Aggregation.class).asEagerSingleton();
        binder.bind(TestResource.class);
        // V1 Resources
        binder.bind(BaseResource.class);
        binder.bind(GroupResource.class);
        binder.bind(JobResource.class);
    }
}
