package gov.cms.dpc.web;

import com.google.inject.Binder;
import com.google.inject.Provides;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import gov.cms.dpc.aggregation.AggregationEngine;
import gov.cms.dpc.common.interfaces.AttributionEngine;
import gov.cms.dpc.web.annotations.AttributionService;
import gov.cms.dpc.web.client.AttributionServiceClient;
import gov.cms.dpc.web.resources.TestResource;
import gov.cms.dpc.web.resources.v1.BaseResource;
import gov.cms.dpc.web.resources.v1.GroupResource;
import gov.cms.dpc.web.resources.v1.JobResource;
import io.dropwizard.client.JerseyClientBuilder;

import javax.ws.rs.client.WebTarget;

public class DPCAppModule extends DropwizardAwareModule<DPWebConfiguration> {

    DPCAppModule() {
        // Not used
    }

    @Override
    public void configure(Binder binder) {

        // Clients
        binder.bind(AttributionEngine.class)
                .to(AttributionServiceClient.class);

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
    @AttributionService
    public WebTarget provideHttpClient() {
        return new JerseyClientBuilder(getEnvironment())
                .using(getConfiguration().getHttpClient())
                .build("service-provider")
                // FIXME(nickrobison): This needs to fixed and pulled from the config
                .target("http://localhost:3272/v1/");
    }
}
