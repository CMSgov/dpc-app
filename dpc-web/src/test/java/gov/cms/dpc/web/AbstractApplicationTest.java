package gov.cms.dpc.web;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.dpc.web.annotations.IntegrationTest;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

@IntegrationTest
public class AbstractApplicationTest {

    protected static final DropwizardTestSupport<DPWebConfiguration> APPLICATION = new DropwizardTestSupport<>(DPCWebApplication.class, null, ConfigOverride.config("server.applicationConnectors[0].port", "7777"));
    protected FhirContext ctx;

    protected AbstractApplicationTest() {
    }

    @BeforeAll
    public static void setup() {
        APPLICATION.before();
    }

    @BeforeEach
    public void createContext() {
        ctx = FhirContext.forR4();
    }

    @AfterAll
    public static void shutdown() {
        APPLICATION.after();
    }

}


