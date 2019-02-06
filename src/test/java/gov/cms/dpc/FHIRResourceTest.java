package gov.cms.dpc;

import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FHIRResourceTest extends AbstractApplicationTest {


    FHIRResourceTest() {
        super();
    }

    @Test
    public void testDataRequest() {
        final IGenericClient client = ctx.newRestfulGenericClient(String.format("http://localhost:%s/v1", APPLICATION.getLocalPort()));

        final Parameters execute = client
                .operation()
                .onInstanceVersion(new IdDt("Group", "1"))
                .named("$export")
                .withNoParameters(Parameters.class)
                .encodedJson()
                .execute();

        final Bundle responseBundle = (Bundle) execute.getParameter().get(0).getResource();

        assertEquals(1, responseBundle.getTotal(), "Should only have 1 resource");
    }
}
