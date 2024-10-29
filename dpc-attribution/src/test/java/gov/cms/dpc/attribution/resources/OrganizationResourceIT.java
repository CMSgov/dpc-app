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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static gov.cms.dpc.common.utils.SeedProcessor.createBaseAttributionGroup;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Organization resource handling")
class OrganizationResourceTest extends AbstractAttributionTest {

        final IGenericClient client;
        final List<Organization> organizationsToCleanUp;

    private OrganizationResourceTest() {
        client = AttributionTestHelpers.createFHIRClient(ctx, getServerURL());
        organizationsToCleanUp = new ArrayList<>();
    }

    @AfterEach
    public void cleanup() {
        organizationsToCleanUp.forEach(organization -> {
            try {
                client
                        .delete()
                        .resourceById(new IdType(organization.getId()))
                        .encodedJson()
                        .execute();
            } catch (Exception e) {
                //ignore
            }

        });
        organizationsToCleanUp.clear();
    }

    @Test
    @DisplayName("Basic registration  🥳")
    void testBasicRegistration() {
        final Organization organization = OrganizationHelpers.createOrganization(ctx, client);
        assertAll(() -> assertNotNull(organization, "Should have an org back"),
                () -> assertFalse(organization.getEndpoint().isEmpty(), "Should have endpoints"));
        organizationsToCleanUp.add(organization);
    }

    @Test
    @DisplayName("Register with invalid organization 🤮")
    void testInvalidOrganization() {

        // Create fake organization with missing data
        final Organization resource = new Organization();
        resource.addIdentifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue("test-mbi");

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
    @DisplayName("Register with unnamed parameter 🤮")
    void testUnnamedParameterSubmission() {

        // Create fake organization with missing data
        final Organization resource = new Organization();
        resource.addIdentifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue("test-mbi");

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
    @DisplayName("Delete organization 🥳")
    void testOrgDeletion() {
        final Organization organization = OrganizationHelpers.createOrganization(ctx, client, "1234567992", false);
        // Add a fake provider and practitioner

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
    @DisplayName("Update organization 🥳")
    void testUpdateOrganization() {
        Organization organization = OrganizationHelpers.createOrganization(ctx, client, "1632101113", false);

        Identifier identifier = new Identifier();
        identifier.setSystem(DPCIdentifierSystem.NPPES.getSystem());
        identifier.setValue("1234567893");
        organization.setIdentifier(Collections.singletonList(identifier));
        organization.setName("An Updated Organization");

        MethodOutcome outcome = client.update().resource(organization).execute();
        Organization orgResult = (Organization) outcome.getResource();

        assertTrue(organization.equalsDeep(orgResult));

        organization.setName("<script>nope</script");
        assertThrows(InvalidRequestException.class, () -> client
                .update()
                .resource(organization)
                .execute(), "Should not have updated organization");
        organizationsToCleanUp.add(organization);
    }

    @Test
    @DisplayName("Delete an organization with a duplicate NPI 🤮")
    void testUpdateOrganizationWithDuplicateNPI() {
        Organization organization1 = OrganizationHelpers.createOrganization(ctx, client, "1633101112", true);
        Organization organization2 = OrganizationHelpers.createOrganization(ctx, client, "1235567892", false);

        Identifier identifier = new Identifier();
        identifier.setSystem(DPCIdentifierSystem.NPPES.getSystem());
        identifier.setValue("1633101112");
        assertEquals(organization1.getIdentifierFirstRep().getId(), organization2.getIdentifierFirstRep().getId());

        organization2.setIdentifier(Collections.singletonList(identifier));
        IUpdateTyped update = client.update().resource(organization2);
        assertThrows(InternalErrorException.class, update::execute);
        organizationsToCleanUp.add(organization1);
        organizationsToCleanUp.add(organization2);
    }

    @Test
    @DisplayName("Get organization by ID 🥳")
    void testGetOrganizationsByIds() {
        List<String> ids = new ArrayList<String>();
        Organization organization1 = OrganizationHelpers.createOrganization(ctx, client, "1633101112", true);
        Organization organization2 = OrganizationHelpers.createOrganization(ctx, client, "1235567892", false);
        ids.add(organization1.getIdentifierFirstRep().getValue());
        ids.add(organization2.getIdentifierFirstRep().getValue());

        Map<String, List<String>> searchParams = new HashMap<>();
        searchParams.put("identifier", Collections.singletonList("id|"+organization1.getIdentifierFirstRep().getValue()+","+organization2.getIdentifierFirstRep().getValue()));
        final Bundle organizations = client
                .search()
                .forResource(Organization.class)
                .whereMap(searchParams)
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        assertEquals(ids.size(), organizations.getEntry().size());
        organizationsToCleanUp.add(organization1);
        organizationsToCleanUp.add(organization2);
    }

    private Practitioner createFakePractitioner(Organization organization) {
        final Practitioner practitioner = new Practitioner();
        practitioner.addName().setFamily("Test").addGiven("Practitioner");
        practitioner.addIdentifier().setSystem(DPCIdentifierSystem.NPPES.getSystem()).setValue("2222222228");

        final Meta meta = new Meta();
        meta.addTag(DPCIdentifierSystem.DPC.getSystem(), organization.getIdElement().getIdPart(), "Organization ID");
        practitioner.setMeta(meta);

        return practitioner;
    }

    private Patient createFakePatient(Organization organization) {
        final Patient patient = new Patient();
        patient.addName().setFamily("Test").addGiven("Patient");
        patient.addIdentifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue("0ZZ0ZZ0ZZ00");
        patient.setBirthDate(Date.valueOf("1990-01-02"));
        patient.setGender(Enumerations.AdministrativeGender.MALE);
        patient.setManagingOrganization(new Reference(organization.getId()));

        return patient;
    }
}
