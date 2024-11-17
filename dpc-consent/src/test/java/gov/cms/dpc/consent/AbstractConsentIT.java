package gov.cms.dpc.consent;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.squarespace.jersey2.guice.JerseyGuiceUtils;
import gov.cms.dpc.consent.resources.UnitTestModule;
import gov.cms.dpc.testing.IntegrationTest;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

@IntegrationTest
public abstract class AbstractConsentIT {
    protected static final String configPath = "src/test/resources/test.application.yml";

    protected static final DropwizardTestSupport<DPCConsentConfiguration> APPLICATION =
            new DropwizardTestSupport<>(DPCConsentService.class, configPath,
                    ConfigOverride.config("server.applicationConnectors[0].port", "6543"),
                    ConfigOverride.config("server.adminConnectors[0].port", "6544"));

    protected FhirContext ctx = FhirContext.forDstu3();

    @BeforeAll
    public static void initDB() throws Exception {
        APPLICATION.before();
//        SharedConfigurationState.clear();
        APPLICATION.getApplication().run("db", "migrate", configPath);
//        SharedConfigurationState.clear();
        APPLICATION.getApplication().run("seed", configPath);

        Injector injector = Guice.createInjector(new UnitTestModule(null));
        JerseyGuiceUtils.install(injector);
    }

    @AfterAll
    public static void shutdown() {
        APPLICATION.after();
        JerseyGuiceUtils.reset();
    }

    // supporting a v1 path might require inserting something like attribution.resources.AbstractAttributionResource
    protected String getServerURL() {
        return String.format("http://localhost:%s/v1", APPLICATION.getLocalPort());
    }

    public static IGenericClient createFHIRClient(FhirContext ctx, String serverURL) {
        ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        IGenericClient client = ctx.newRestfulGenericClient(serverURL);

        // Disable logging for tests
        LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
        loggingInterceptor.setLogRequestSummary(false);
        loggingInterceptor.setLogResponseSummary(false);
        client.registerInterceptor(loggingInterceptor);

        return client;
    }
}
