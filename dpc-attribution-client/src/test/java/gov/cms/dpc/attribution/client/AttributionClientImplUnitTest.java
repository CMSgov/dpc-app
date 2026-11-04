package gov.cms.dpc.attribution.client;

import ca.uhn.fhir.context.FhirContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AttributionClientImplUnitTest {

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();

    private final AttributionClient client = new AttributionClientImpl(
            FHIR_CONTEXT.newRestfulGenericClient("http://localhost:3500/r4/"));

    @Test
    void checkHealthThrowsUnsupportedOperationException() {
        assertNotImplemented(client::checkHealth);
    }

    @Test
    void getVersionThrowsUnsupportedOperationException() {
        assertNotImplemented(client::getVersion);
    }

    private static void assertNotImplemented(Executable invocation) {
        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, invocation);
        assertEquals("Method not implemented yet.", exception.getMessage());
    }
}
