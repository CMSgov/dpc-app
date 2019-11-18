package gov.cms.dpc.consent;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public abstract class AbstractConsentTest {
    private static final String KEY_PREFIX = "dpc.consent";
    protected static final DropwizardTestSupport<DPCConsentConfiguration> APPLICATION = new DropwizardTestSupport<>(DPCConsentService.class, null, ConfigOverride.config(KEY_PREFIX, "", ""));

    protected FhirContext ctx = FhirContext.forDstu3();

    @BeforeAll
    public static void initDB() throws Exception {
        APPLICATION.before();
        APPLICATION.getApplication().run("db", "migrate");
        APPLICATION.getApplication().run("seed");
    }

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
        return ctx.newRestfulGenericClient(serverURL);
    }
}
