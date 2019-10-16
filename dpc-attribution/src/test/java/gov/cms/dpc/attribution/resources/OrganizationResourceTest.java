package gov.cms.dpc.attribution.resources;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import gov.cms.dpc.attribution.AbstractAttributionTest;
import gov.cms.dpc.attribution.AttributionTestHelpers;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Date;

import static org.junit.jupiter.api.Assertions.*;

class OrganizationResourceTest extends AbstractAttributionTest {

    private OrganizationResourceTest() {
        // Not used
    }

    @Test
    void testBasicRegistration() {
        final Organization organization = AttributionTestHelpers.createOrganization(ctx, getServerURL());
        assertAll(() -> assertNotNull(organization, "Should have an org back"),
                () -> assertFalse(organization.getEndpoint().isEmpty(), "Should have endpoints"));
    }

    @Test
    void testInvalidOrganization() {

        // Create fake organization with missing data
        final Organization resource = new Organization();
        resource.addIdentifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue("test-mbi");

        final IGenericClient client = AttributionTestHelpers.createFHIRClient(ctx, getServerURL());

        final Parameters parameters = new Parameters();
        parameters.addParameter().setResource(resource);

        final var submit = client
                .operation()
                .onType(Organization.class)
                .named("submit")
                .withParameters(parameters)
                .returnResourceType(Organization.class)
                .encodedJson();

        assertThrows(InternalErrorException.class, submit::execute, "Should throw an internal server error");
    }

    @Test
    void testOrgDeletion() {
        final Organization organization = AttributionTestHelpers.createOrganization(ctx, getServerURL());
        // Add a fake provider and practitioner
        final IGenericClient client = AttributionTestHelpers.createFHIRClient(ctx, getServerURL());

        final Practitioner practitioner = createFakePractitioner(organization);
        final MethodOutcome practCreated = client
                .create()
                .resource(practitioner)
                .encodedJson()
                .execute();

        assertTrue(practCreated.getCreated(), "Should have been created");
        final Practitioner createdPractitioner = (Practitioner) practCreated.getResource();

        final Patient patient = createFakePatient(organization);
        final MethodOutcome patCreated = client
                .create()
                .resource(patient)
                .encodedJson()
                .execute();

        final Patient createdPatient = (Patient) patCreated.getResource();
        assertTrue(patCreated.getCreated(), "Should have been created");

        // Then delete the organization

        client
                .delete()
                .resourceById(new IdType(organization.getId()))
                .encodedJson()
                .execute();

        // Try to read the resources, should get 404s

        assertThrows(ResourceNotFoundException.class, () -> client
                .read()
                .resource(Patient.class)
                .withId(createdPatient.getId())
                .encodedJson()
                .execute(), "Should not have found patient");

        assertThrows(ResourceNotFoundException.class, () -> client
                .read()
                .resource(Practitioner.class)
                .withId(createdPractitioner.getId())
                .encodedJson()
                .execute(), "Should not have found practitioner");

        assertThrows(ResourceNotFoundException.class, () -> client
                .read()
                .resource(Organization.class)
                .withId(organization.getId())
                .encodedJson()
                .execute(), "Should not have found organization");
    }

    private Practitioner createFakePractitioner(Organization organization) {
        final Practitioner practitioner = new Practitioner();
        practitioner.addName().setFamily("Test").addGiven("Practitioner");
        practitioner.addIdentifier().setSystem(DPCIdentifierSystem.NPPES.getSystem()).setValue("fake-prov-npi");

        final Meta meta = new Meta();
        meta.addTag(DPCIdentifierSystem.DPC.getSystem(), organization.getIdElement().getIdPart(), "Organization ID");
        practitioner.setMeta(meta);

        return practitioner;
    }

    private Patient createFakePatient(Organization organization) {
        final Patient patient = new Patient();
        patient.addName().setFamily("Test").addGiven("Patient");
        patient.addIdentifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue("test-fake-mbi");
        patient.setBirthDate(Date.valueOf("1990-01-02"));
        patient.setManagingOrganization(new Reference(organization.getId()));

        return patient;
    }
}
