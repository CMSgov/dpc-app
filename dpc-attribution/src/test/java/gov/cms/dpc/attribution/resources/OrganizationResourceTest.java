package gov.cms.dpc.attribution.resources;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IUpdateTyped;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import gov.cms.dpc.attribution.AbstractAttributionTest;
import gov.cms.dpc.attribution.AttributionTestHelpers;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.testing.OrganizationHelpers;
import org.hl7.fhir.dstu3.model.*;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.util.Collections;

import static gov.cms.dpc.common.utils.SeedProcessor.createBaseAttributionGroup;
import static org.junit.jupiter.api.Assertions.*;

class OrganizationResourceTest extends AbstractAttributionTest {

    private OrganizationResourceTest() {
        // Not used
    }

    @Test
    void testBasicRegistration() {
        final Organization organization = OrganizationHelpers.createOrganization(ctx, AttributionTestHelpers.createFHIRClient(ctx, getServerURL()));
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
        parameters.addParameter().setResource(resource).setName("resource");

        final var submit = client
                .operation()
                .onType(Organization.class)
                .named("submit")
                .withParameters(parameters)
                .returnResourceType(Organization.class)
                .encodedJson();

        assertThrows(InvalidRequestException.class, submit::execute, "Should throw an error for not supporting Organizations");
    }

    @Test
    void testUnnamedParameterSubmission() {

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

        assertThrows(InvalidRequestException.class, submit::execute, "Should throw an error for not supporting Organizations");
    }

    @Test
    void testOrgDeletion() {
        final Organization organization = OrganizationHelpers.createOrganization(ctx, AttributionTestHelpers.createFHIRClient(ctx, getServerURL()));
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

        // Attribute them
        final Group newRoster = createBaseAttributionGroup(FHIRExtractors.getProviderNPI(createdPractitioner), organization.getIdElement().getIdPart());
        final Reference patientReference = new Reference(createdPatient.getId());
        newRoster.addMember().setEntity(patientReference);

        final Parameters addParam = new Parameters();
        addParam.addParameter().setResource(newRoster);

        // Update the roster
        client
                .create()
                .resource(newRoster)
                .encodedJson()
                .execute();

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

    @Test
    void testUpdateOrganization() {
        final IGenericClient client = AttributionTestHelpers.createFHIRClient(ctx, getServerURL());
        Organization organization = OrganizationHelpers.createOrganization(ctx, AttributionTestHelpers.createFHIRClient(ctx, getServerURL()), "test-update-organization", false);

        Identifier identifier = new Identifier();
        identifier.setSystem(DPCIdentifierSystem.NPPES.getSystem());
        identifier.setValue("UPDATED012345");
        organization.setIdentifier(Collections.singletonList(identifier));
        organization.setName("An Updated Organization");

        MethodOutcome outcome = client.update().resource(organization).execute();
        Organization orgResult = (Organization) outcome.getResource();

        assertTrue(organization.equalsDeep(orgResult));
    }

    @Test
    void testUpdateOrganizationWithDuplicateNPI() {
        final IGenericClient client = AttributionTestHelpers.createFHIRClient(ctx, getServerURL());
        OrganizationHelpers.createOrganization(ctx, AttributionTestHelpers.createFHIRClient(ctx, getServerURL()), "org-update-npi-duplicate1", false);
        Organization organization2 = OrganizationHelpers.createOrganization(ctx, AttributionTestHelpers.createFHIRClient(ctx, getServerURL()), "org-update-npi-duplicate2", false);

        Identifier identifier = new Identifier();
        identifier.setSystem(DPCIdentifierSystem.NPPES.getSystem());
        identifier.setValue("org-update-npi-duplicate1");

        organization2.setIdentifier(Collections.singletonList(identifier));
        IUpdateTyped update = client.update().resource(organization2);
        assertThrows(InvalidRequestException.class, update::execute);
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
        patient.setGender(Enumerations.AdministrativeGender.MALE);
        patient.setManagingOrganization(new Reference(organization.getId()));

        return patient;
    }
}
