package gov.cms.dpc.attribution.client;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.IRestfulClientFactory;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import gov.cms.dpc.attribution.client.config.AttributionBundleConfiguration;
import gov.cms.dpc.fhir.configuration.FHIRClientConfiguration;
import io.dropwizard.core.Configuration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AttributionClientModuleUnitTest {

    private static final Key<IGenericClient> CLIENT_KEY = Key.get(IGenericClient.class, Names.named("attributionR4"));
    private static final Key<FhirContext> CONTEXT_KEY = Key.get(FhirContext.class, Names.named("fhirContextAttributionR4"));

    @Test
    void moduleProvidesSingleton() {
        TestConfiguration configuration = new TestConfiguration();
        configuration.getAttributionClientConfiguration().setServerBaseUrl("http://fakeurl");
        AttributionClientModule<TestConfiguration> module = new AttributionClientModule<>();
        module.setConfiguration(configuration);

        Injector injector = Guice.createInjector(module);

        // Should return a Singleton.  I.E., always the same instance
        assertSame(injector.getInstance(AttributionClient.class), injector.getInstance(AttributionClient.class));
        assertEquals("http://fakeurl", injector.getInstance(CLIENT_KEY).getServerBase());
        assertSame(injector.getInstance(CLIENT_KEY), injector.getInstance(CLIENT_KEY));
        assertSame(injector.getInstance(CONTEXT_KEY), injector.getInstance(CONTEXT_KEY));
    }

    @Test
    void moduleUsesConnectionSettings() {
        FHIRClientConfiguration configuration = new FHIRClientConfiguration();
        configuration.setServerBaseUrl("http://fakeurl");
        configuration.getTimeouts().setConnectionTimeout(1234);
        configuration.getTimeouts().setSocketTimeout(2345);
        configuration.getTimeouts().setRequestTimeout(3456);
        configuration.getConnectionPoolConfiguration().setPoolMaxPerRoute(7);
        configuration.getConnectionPoolConfiguration().setPoolMaxTotal(13);

        Injector injector = Guice.createInjector(new AttributionClientModule<TestConfiguration>(configuration));
        IGenericClient client = injector.getInstance(CLIENT_KEY);
        IRestfulClientFactory factory = client.getFhirContext().getRestfulClientFactory();

        assertEquals("http://fakeurl", client.getServerBase());
        assertEquals(FhirVersionEnum.R4, client.getFhirContext().getVersion().getVersion());
        assertEquals(EncodingEnum.JSON, client.getEncoding());
        assertEquals(ServerValidationModeEnum.NEVER, factory.getServerValidationMode());
        assertEquals(1234, factory.getConnectTimeout());
        assertEquals(2345, factory.getSocketTimeout());
        assertEquals(3456, factory.getConnectionRequestTimeout());
        assertEquals(7, factory.getPoolMaxPerRoute());
        assertEquals(13, factory.getPoolMaxTotal());
    }

    @Test
    // TODO: Delete when we get rid of stu3
    // This test makes sure injecting a new attribution FHIR client doesn't break our existing STU3 FHIR client.
    // Get rid of it once we remove STU3.
    void makeSureMultipleFhirContextsWork() {
        FhirContext stu3 = FhirContext.forDstu3();
        FhirContext sharedR4 = FhirContext.forR4();
        IGenericClient existingClient = stu3.newRestfulGenericClient("http://fakeurl/v1/");
        Injector injector = Guice.createInjector(
                new AttributionClientModule<TestConfiguration>(createClientConfiguration()),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(FhirContext.class).annotatedWith(Names.named("r4")).toInstance(sharedR4);
                        bind(IGenericClient.class).annotatedWith(Names.named("attribution")).toInstance(existingClient);
                    }
                });

        // Getting the "attribution" client should still return the generic stu3 FHIR client we created above
        assertSame(existingClient, injector.getInstance(Key.get(IGenericClient.class, Names.named("attribution"))));

        // When we ask for an "attributionR4" client we should not get teh existingClient we created above
        assertNotSame(existingClient, injector.getInstance(CLIENT_KEY));

        // Asking for a "fhirContextAttributionR4" context should be different from our stu3 and sharedR4 above
        assertNotSame(stu3, injector.getInstance(CONTEXT_KEY));
        assertNotSame(sharedR4, injector.getInstance(CONTEXT_KEY));
    }

    private static FHIRClientConfiguration createClientConfiguration() {
        FHIRClientConfiguration configuration = new FHIRClientConfiguration();
        configuration.setServerBaseUrl("http://attribution.example/r4/");
        configuration.getTimeouts().setConnectionTimeout(20000);
        configuration.getTimeouts().setSocketTimeout(20000);
        configuration.getTimeouts().setRequestTimeout(20000);
        configuration.getConnectionPoolConfiguration().setPoolMaxPerRoute(20);
        configuration.getConnectionPoolConfiguration().setPoolMaxTotal(20);
        return configuration;
    }

    private static class TestConfiguration extends Configuration implements AttributionBundleConfiguration {
        private final FHIRClientConfiguration clientConfiguration = createClientConfiguration();

        @Override
        public FHIRClientConfiguration getAttributionClientConfiguration() {
            return clientConfiguration;
        }
    }
}
