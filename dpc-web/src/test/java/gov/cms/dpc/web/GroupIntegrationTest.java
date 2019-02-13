package gov.cms.dpc.web;

import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.NonFhirResponseException;
import ca.uhn.fhir.rest.gclient.IOperationUntypedWithInput;
import ca.uhn.fhir.rest.server.exceptions.UnclassifiedServerFailureException;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple integration tests for Group resource
 */
public class GroupIntegrationTest extends AbstractApplicationTest {

    /**
     * Test that the group resource correctly accepts/rejects invalid content types.
     */
    @Test
    public void testMediaTypes() {

        // Verify that FHIR is accepted
        final IGenericClient client = ctx.newRestfulGenericClient("http://localhost:" + APPLICATION.getLocalPort() + "/v1/");

        final IOperationUntypedWithInput<Parameters> execute = client
                .operation()
                .onInstanceVersion(new IdDt("Group", "1"))
                .named("$export")
                .withNoParameters(Parameters.class)
                .useHttpGet();


        assertThrows(NonFhirResponseException.class, execute::execute, "Should throw exception, but accept JSON request");

        // Try again, but use a different encoding
        assertThrows(UnclassifiedServerFailureException.class, () -> execute.encodedXml().execute(), "Should not accept XML encoding");
    }

    @Test
    public void testFHIRMarshaling() throws IOException {

        final Group group = new Group();
        group.addIdentifier().setValue("Group/test");

        final IGenericClient client = ctx.newRestfulGenericClient("http://localhost:" + APPLICATION.getLocalPort() + "/v1/");

        MethodOutcome execute = client
                .create()
                .resource(group)
                .encodedJson()
                .preferResponseType(Patient.class)
                .execute();

        final Patient resource = (Patient) execute.getResource();
        assertAll(() -> assertNotNull(resource, "Should have patient resource"),
                () -> assertEquals("test-id", resource.getIdentifierFirstRep().getValue(), "ID's should match"),
                () -> assertEquals("Doe", resource.getNameFirstRep().getFamily(), "Should have updated family name"),
                () -> assertEquals("John", resource.getNameFirstRep().getGivenAsSingleString(), "Should have updated given name"));

        // Try to something that should fail
        final Group g2 = new Group();

        g2.addIdentifier().setValue("Group/fail");

        final HttpPost post = new HttpPost("http://localhost:" + APPLICATION.getLocalPort() + "/v1/Group");
        final IParser parser = ctx.newJsonParser();
        post.setEntity(new StringEntity(parser.encodeResourceToString(g2)));

        final CloseableHttpClient c2 = HttpClients.createDefault();

        try (CloseableHttpResponse response = c2.execute(post)) {

            assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatusLine().getStatusCode(), "Should have 500 error");

            // Try to get the operation outcome
            final OperationOutcome oo = (OperationOutcome) parser.parseResource(EntityUtils.toString(response.getEntity()));
            assertAll(() -> assertEquals(1, oo.getIssue().size(), "Should have 1 issue"),
                    () -> assertEquals(OperationOutcome.IssueSeverity.FATAL, oo.getIssueFirstRep().getSeverity(), "Should be fatal"),
                    () -> assertEquals("Should fail", oo.getIssueFirstRep().getDetails().getText(), "Should have matching error"));
        }
    }
}
