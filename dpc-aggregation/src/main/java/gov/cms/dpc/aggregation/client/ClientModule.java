package gov.cms.dpc.aggregation.client;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.IRestfulClientFactory;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import gov.cms.dpc.aggregation.DPCAggregationConfiguration;
import gov.cms.dpc.aggregation.client.attribution.AttributionClient;
import gov.cms.dpc.aggregation.client.attribution.AttributionClientImpl;
import gov.cms.dpc.aggregation.client.consent.ConsentClient;
import gov.cms.dpc.aggregation.client.consent.ConsentClientImpl;

import javax.inject.Named;

public class ClientModule extends PrivateModule {

    private final DPCAggregationConfiguration config;
    private final IRestfulClientFactory factory;

    public ClientModule(DPCAggregationConfiguration config, FhirContext ctx) {
        this.config = config;
        this.factory = ctx.getRestfulClientFactory();
        this.factory.setServerValidationMode(ServerValidationModeEnum.NEVER);
    }


    @Override
    protected void configure() {
        bind(ConsentClient.class).to(ConsentClientImpl.class);
        bind(AttributionClient.class).to(AttributionClientImpl.class);

        expose(ConsentClient.class);
        expose(AttributionClient.class);
    }

    @Provides
    @Named("consent")
    IGenericClient provideConsentClient() {
        return this.factory.newGenericClient(this.config.getConsentService());
    }

    @Provides
    @Named("attribution")
    IGenericClient provideAttributionClient() {
        return this.factory.newGenericClient(this.config.getAttributionService());
    }
}
