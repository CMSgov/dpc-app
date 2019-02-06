package gov.cms.dpc;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FHIRResourceTest {

    private static final DropwizardTestSupport<DPCAppConfiguration> APPLICATION = new DropwizardTestSupport<>(DPCAppApplication.class, null, ConfigOverride.config("server.applicationConnectors[0].port", "0"));
    private static FhirContext ctx;

    public FHIRResourceTest() {
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

    @Test
    public void testDataRequest() {
        final IGenericClient client = ctx.newRestfulGenericClient(String.format("http://localhost:%s/v1", APPLICATION.getLocalPort()));

        final Parameters execute = client
                .operation()
                .onInstanceVersion(new IdDt("Group", "1"))
                .named("$export")
                .withNoParameters(Parameters.class)
                .execute();

        final Bundle responseBundle = (Bundle) execute.getParameter().get(0).getResource();

        assertEquals(1, responseBundle.getTotal(), "Should only have 1 resource");
    }
}
