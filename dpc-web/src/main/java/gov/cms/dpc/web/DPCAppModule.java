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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.ws.rs.client.WebTarget;

public class DPCAppModule extends DropwizardAwareModule<DPWebConfiguration> {

    private static final Logger logger = LoggerFactory.getLogger(DPCAppModule.class);

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
    @Singleton
    public WebTarget provideHttpClient() {
        logger.debug("Connecting to attribution server at {}.", getConfiguration().getAttributionURL());
        return new JerseyClientBuilder(getEnvironment())
                .using(getConfiguration().getHttpClient())
                .build("attribution-service")
                .target(getConfiguration().getAttributionURL());
    }
}
