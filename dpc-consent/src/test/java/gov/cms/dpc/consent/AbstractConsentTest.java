package gov.cms.dpc.consent;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import gov.cms.dpc.testing.IntegrationTest;
import io.dropwizard.testing.DropwizardTestSupport;
import org.junit.jupiter.api.AfterAll;

@IntegrationTest
public abstract class AbstractConsentTest {
    protected static final String configPath = "src/test/resources/ci.application.yml";

    protected static final DropwizardTestSupport<DPCConsentConfiguration> APPLICATION =
            new DropwizardTestSupport<>(DPCConsentService.class, configPath);

    protected FhirContext ctx = FhirContext.forDstu3();

    @AfterAll
    public static void shutdown() {
        APPLICATION.after();
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
