package gov.cms.dpc.api.resources;

import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IOperationUntypedWithInput;
import ca.uhn.fhir.rest.server.exceptions.UnclassifiedServerFailureException;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.SchemaBaseValidator;
import ca.uhn.fhir.validation.schematron.SchematronBaseValidator;
import gov.cms.dpc.api.AbstractApplicationTest;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.hl7.fhir.dstu3.model.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple integration tests for Group resource
 */
public class GroupResourceTest extends AbstractApplicationTest {

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

        // Execute using fhir+xml (which we don't support)
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

    @Test
    void testGroupPost() throws IOException {
        final Group group = new Group();
        group.addIdentifier().setValue("8D80925A-027E-43DD-8AED-9A501CC4CD91");
        group.setType(Group.GroupType.PERSON);
        group.setActual(true);
        // Add some patients
        final Patient p1 = new Patient();
        p1.addIdentifier().setValue("20000000000890");
        p1.addName().addGiven("Tester 1").setFamily("Patient");
        p1.setBirthDate(new GregorianCalendar(2019, Calendar.MARCH, 1).getTime());
        final Patient p2 = new Patient();
        p2.addIdentifier().setValue("20000000000889");
        p2.addName().addGiven("Tester 2").setFamily("Patient");
        p2.setBirthDate(new GregorianCalendar(2019, Calendar.MARCH, 1).getTime());

        group.addMember().setEntity(new Reference().setIdentifier(p2.getIdentifierFirstRep()));
        group.addMember().setEntity(new Reference().setIdentifier(p2.getIdentifierFirstRep()));

        // Post it

        final FhirValidator validator = ctx.newValidator();

        final SchemaBaseValidator val1 = new SchemaBaseValidator(ctx);
        final SchematronBaseValidator val2 = new SchematronBaseValidator(ctx);
        validator.registerValidatorModule(val1);
        validator.registerValidatorModule(val2);

        final HttpPost post = new HttpPost("http://localhost:" + APPLICATION.getLocalPort() + "/v1/Group/1/$export");
        final IParser parser = ctx.newJsonParser();
        post.setEntity(new StringEntity(parser.encodeResourceToString(group)));

        final CloseableHttpClient client = HttpClients.createDefault();

        try (CloseableHttpResponse response = client.execute(post)) {
            assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatusLine().getStatusCode(), "Should have passed");
            final OperationOutcome outcome = (OperationOutcome) parser.parseResource(EntityUtils.toString(response.getEntity()));
            assertEquals(2, outcome.getIssue().size(), "Should have 2 401s");
        }
    }
}
