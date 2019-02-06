package gov.cms.dpc;

import ca.uhn.fhir.context.FhirContext;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

public class AbstractApplicationTest {

    static final DropwizardTestSupport<DPCAppConfiguration> APPLICATION = new DropwizardTestSupport<>(DPCAppApplication.class, null, ConfigOverride.config("server.applicationConnectors[0].port", "0"));
    static FhirContext ctx;

    AbstractApplicationTest() {
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


