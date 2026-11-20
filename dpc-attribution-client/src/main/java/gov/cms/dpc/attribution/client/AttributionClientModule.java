package gov.cms.dpc.attribution.client;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.apache.ApacheHttp5RestfulClientFactory;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.IRestfulClientFactory;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import com.google.inject.Provides;
import gov.cms.dpc.attribution.client.config.AttributionBundleConfiguration;
import gov.cms.dpc.attribution.client.service.ConsentService;
import gov.cms.dpc.common.hibernate.consent.DPCConsentManagedSessionFactory;
import gov.cms.dpc.common.jdbi.ConsentDAO;
import gov.cms.dpc.fhir.configuration.ConnectionPoolConfiguration;
import gov.cms.dpc.fhir.configuration.FHIRClientConfiguration;
import gov.cms.dpc.fhir.configuration.TimeoutConfiguration;
import io.dropwizard.core.Configuration;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.HttpEntityContainer;
import org.apache.hc.core5.http.io.entity.BufferedHttpEntity;
import ru.vyarus.dropwizard.guice.module.support.DropwizardAwareModule;

import java.util.Objects;

/**
 * Provides an R4 attribution client using the host application's configuration.
 *
 * @param <T> Dropwizard configuration exposing attribution client settings
 */
public class AttributionClientModule<T extends Configuration & AttributionBundleConfiguration>
        extends DropwizardAwareModule<T> {

    private FHIRClientConfiguration clientConfiguration;

    public AttributionClientModule() {
        // Read settings from the host Dropwizard application during configuration.
    }

    public AttributionClientModule(FHIRClientConfiguration clientConfiguration) {
        this.clientConfiguration = Objects.requireNonNull(clientConfiguration);
    }

    @Override
    public void configure() {
        if (clientConfiguration == null) {
            clientConfiguration = configuration().getAttributionClientConfiguration();
        }
    }

    @Provides
    @Singleton
    public AttributionClient provideAttributionClient(@Named("attributionR4") IGenericClient client, ConsentService consentService) {
        return new AttributionClient(client, consentService);
    }

    @Provides
    @Singleton
    public ConsentService provideConsentService(DPCConsentManagedSessionFactory factory) {
        return new ConsentService(new ConsentDAO(factory), clientConfiguration.getServerBaseUrl());
    }

    @Provides
    @Singleton
    @Named("fhirContextAttributionR4")
    // We need a new FHIRContext outside of the one provided by FHIRModule because we're going to update its
    // ClientFactory with settings specific to calling dpc-attribution.
    public FhirContext provideAttributionContext() {
        FhirContext context = FhirContext.forR4();
        context.setRestfulClientFactory(new ApacheHttp5RestfulClientFactory(context) {
            @Override
            protected HttpClientBuilder getHttpClientBuilder() {
                // HAPI and Apache don't play nice together.  Apache sometimes closes its HTTP stream before HAPI reads
                // it, so it needs to be made repeatable to ensure HAPI can always read it.
                return super.getHttpClientBuilder().addResponseInterceptorFirst((response, details, httpContext) -> {
                    if (response instanceof HttpEntityContainer container && container.getEntity() != null
                            && !container.getEntity().isRepeatable()) {
                        container.setEntity(new BufferedHttpEntity(container.getEntity()));
                    }
                });
            }
        });
        return context;
    }

    @Provides
    @Singleton
    @Named("attributionR4")
    public IGenericClient provideFHIRClient(@Named("fhirContextAttributionR4") FhirContext context) {
        IRestfulClientFactory factory = context.getRestfulClientFactory();
        factory.setServerValidationMode(ServerValidationModeEnum.NEVER);

        TimeoutConfiguration timeouts = clientConfiguration.getTimeouts();
        factory.setConnectTimeout(timeouts.getConnectionTimeout());
        factory.setSocketTimeout(timeouts.getSocketTimeout());
        factory.setConnectionRequestTimeout(timeouts.getRequestTimeout());

        ConnectionPoolConfiguration pool = clientConfiguration.getConnectionPoolConfiguration();
        factory.setPoolMaxPerRoute(pool.getPoolMaxPerRoute());
        factory.setPoolMaxTotal(pool.getPoolMaxTotal());

        IGenericClient client = context.newRestfulGenericClient(clientConfiguration.getServerBaseUrl());
        client.setEncoding(ca.uhn.fhir.rest.api.EncodingEnum.JSON);
        return client;
    }
}
