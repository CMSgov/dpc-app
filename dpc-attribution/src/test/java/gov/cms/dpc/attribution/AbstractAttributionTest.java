package gov.cms.dpc.attribution;

import ca.uhn.fhir.context.FhirContext;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public class AbstractAttributionTest {
    protected static final DropwizardTestSupport<DPCAttributionConfiguration> APPLICATION = new DropwizardTestSupport<>(DPCAttributionService.class, null, ConfigOverride.config("server.applicationConnectors[0].port", "3727"));

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

    protected String getServerURL() {
        return String.format("http://localhost:%s/v1", APPLICATION.getLocalPort());
    }
}
