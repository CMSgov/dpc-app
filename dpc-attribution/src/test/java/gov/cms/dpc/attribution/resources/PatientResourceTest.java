package gov.cms.dpc.attribution.resources;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICreateTyped;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.IUpdateTyped;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import gov.cms.dpc.attribution.AbstractAttributionTest;
import gov.cms.dpc.attribution.AttributionTestHelpers;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.FHIRExtractors;
import org.hl7.fhir.dstu3.model.*;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.util.UUID;

import static gov.cms.dpc.attribution.AttributionTestHelpers.*;
import static gov.cms.dpc.common.utils.SeedProcessor.createBaseAttributionGroup;
import static org.junit.jupiter.api.Assertions.*;

class PatientResourceTest extends AbstractAttributionTest {

    private PatientResourceTest() {
        // Not used
    }

    @Test
    void testPatientReadWrite() {
        final Patient patient = createPatientResource("0O00O00OO00", DEFAULT_ORG_ID);

        final Reference orgReference = new Reference(new IdType("Organization", DEFAULT_ORG_ID));
        patient.setManagingOrganization(orgReference);

        final IGenericClient client = createFHIRClient(ctx, getServerURL());

        final MethodOutcome outcome = client
                .create()
                .resource(patient)
                .encodedJson()
                .execute();

        assertTrue(outcome.getCreated(), "Should have been created");

        final Patient createdResource = (Patient) outcome.getResource();

        // Ensure we have some of the necessary fields
        assertAll(() -> assertEquals(Enumerations.AdministrativeGender.OTHER, createdResource.getGender(), "Should have matching gender"),
                () -> assertTrue(orgReference.equalsDeep(createdResource.getManagingOrganization()), "Should have organization"));

        final Patient fetchedPatient = client
                .read()
                .resource(Patient.class)
                .withId(createdResource.getIdElement())
                .encodedJson()
                .execute();

        assertTrue(createdResource.equalsDeep(fetchedPatient), "Created and fetched should be equal");

        // Try to create them again

        final ICreateTyped secondCreation = client
                .create()
                .resource(patient)
                .encodedJson();

        final MethodOutcome execute = secondCreation.execute();
        assertNull(execute.getCreated(), "Should not be able to create again");
    }

    @Test
    void testPatientMBIPersistedAsUppercase() {
        final Reference orgReference = new Reference(new IdType("Organization", DEFAULT_ORG_ID));
        final String patientMbi = "3aa0C00aA00";
        final Patient patient = createPatientResource(patientMbi, DEFAULT_ORG_ID);
        patient.setManagingOrganization(orgReference);

        final IGenericClient client = createFHIRClient(ctx, getServerURL());
        final MethodOutcome actualOutcome = client
                .create()
                .resource(patient)
                .encodedJson()
                .execute();

        assertNotNull(actualOutcome.getCreated(), "getCreate field should not have been missing.");
        assertTrue(actualOutcome.getCreated(), "New resource should have been created.");

        final Patient patientReturned = (Patient)actualOutcome.getResource();
        assertEquals(patientMbi.toUpperCase(), FHIRExtractors.getPatientMBI(patientReturned), "Expected to receive MBI as uppercase");

        final Patient fetchedPatient = client
                .read()
                .resource(Patient.class)
                .withId(patientReturned.getId())
                .encodedJson()
                .execute();

        assertEquals(patientMbi.toUpperCase(),FHIRExtractors.getPatientMBI(fetchedPatient),"Fetched patient should have had uppercase MBI");
    }

    @Test
    void testCreatePatientWithInvalidMbi() {
        final Patient patient = createPatientResource("not-an-mbi", DEFAULT_ORG_ID);

        final Reference orgReference = new Reference(new IdType("Organization", DEFAULT_ORG_ID));
        patient.setManagingOrganization(orgReference);

        final IGenericClient client = createFHIRClient(ctx, getServerURL());

        ICreateTyped create = client
                .create()
                .resource(patient);

        assertThrows(UnprocessableEntityException.class, create::execute);
    }

    @Test
    void testPatientSearchWithValidOrgAndMbi() {
        final IGenericClient client = createFHIRClient(ctx, getServerURL());

        final Bundle searchResults = client
                .search()
                .forResource(Patient.class)
                .where(Patient.IDENTIFIER.exactly().systemAndCode(DPCIdentifierSystem.MBI.getSystem(), DEFAULT_PATIENT_MBI))
                .and(Patient.ORGANIZATION.hasId("Organization/" + DEFAULT_ORG_ID))
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        assertEquals(1, searchResults.getTotal(), "Should have a single patient");
    }

    @Test
    void testPatientSearchWithInvalidOrg() {
        final IGenericClient client = createFHIRClient(ctx, getServerURL());

        final Bundle searchResults = client
                .search()
                .forResource(Patient.class)
                .where(Patient.IDENTIFIER.exactly().systemAndCode(DPCIdentifierSystem.MBI.getSystem(), DEFAULT_PATIENT_MBI))
                .and(Patient.ORGANIZATION.hasId("Organization/" + UUID.randomUUID().toString()))
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        assertEquals(0, searchResults.getTotal(), "Should not have any patients");
    }

    @Test
    void testPatientSearchWithLowerCaseMbi() {
        final IGenericClient client = createFHIRClient(ctx, getServerURL());

        final Bundle searchResult = client
                .search()
                .forResource(Patient.class)
                .where(Patient.IDENTIFIER.exactly().systemAndCode(DPCIdentifierSystem.MBI.getSystem(), DEFAULT_PATIENT_MBI.toLowerCase()))
                .and(Patient.ORGANIZATION.hasId("Organization/" + UUID.randomUUID().toString()))
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        assertEquals(0, searchResult.getTotal(), "Should not have any patients");
    }

    @Test
    void testPatientDeletion() {
        final IGenericClient client = createFHIRClient(ctx, getServerURL());

        final IQuery<Bundle> firstQuery = client
                .search()
                .forResource(Patient.class)
                .where(Patient.IDENTIFIER.exactly().systemAndCode(DPCIdentifierSystem.MBI.getSystem(), DEFAULT_PATIENT_MBI))
                .and(Patient.ORGANIZATION.hasId("Organization/" + DEFAULT_ORG_ID))
                .returnBundle(Bundle.class)
                .encodedJson();

        final Bundle firstSearch = firstQuery.execute();

        assertEquals(1, firstSearch.getTotal(), "Should have a single patient");

        // Create a practitioner and an attribution resource
        final Practitioner practitioner = AttributionTestHelpers.createPractitionerResource("2222222228");

        final MethodOutcome outcome = client
                .create()
                .resource(practitioner)
                .encodedJson()
                .execute();

        // Add an attribution Group
        final Group group = createBaseAttributionGroup(FHIRExtractors.getProviderNPI(practitioner), DEFAULT_ORG_ID);

        group.addMember().setEntity(new Reference(firstSearch.getEntryFirstRep().getResource().getId()));

        final MethodOutcome creationOutcome = client
                .create()
                .resource(group)
                .encodedJson()
                .execute();

        final Group attributionGroup = (Group) creationOutcome.getResource();

        // Remove the patient and try the search again
        final Patient patient = (Patient) firstSearch.getEntryFirstRep().getResource();

        final MethodOutcome deleteOutcome = client
                .delete()
                .resource(patient)
                .encodedJson()
                .execute();

        assertNull(deleteOutcome, "Should have succeeded with empty outcome");
        // Try the first search again, which should be empty

        final Bundle finalSearch = firstQuery.execute();
        assertEquals(0, finalSearch.getTotal(), "Should not have any patients");

        // Ensure the group still exists
        final Group g2 = client
                .read()
                .resource(Group.class)
                .withId(attributionGroup.getId())
                .encodedJson()
                .execute();

        assertEquals(0, g2.getMember().size(), "Should not have any attributions");
    }

    @Test
    void testPatientUpdate() {
        final IGenericClient client = createFHIRClient(ctx, getServerURL());

        final IQuery<Bundle> firstQuery = client
                .search()
                .forResource(Patient.class)
                .where(Patient.IDENTIFIER.exactly().systemAndCode(DPCIdentifierSystem.MBI.getSystem(), "4S41C00AA00"))
                .and(Patient.ORGANIZATION.hasId("Organization/" + DEFAULT_ORG_ID))
                .returnBundle(Bundle.class)
                .encodedJson();

        final Bundle firstSearch = firstQuery.execute();

        assertEquals(1, firstSearch.getTotal(), "Should have a single patient");

        final Patient foundPatient = (Patient) firstSearch.getEntryFirstRep().getResource();

        final java.util.Date createdAt = foundPatient.getMeta().getLastUpdated();

        // Update the name
        foundPatient.getNameFirstRep().setFamily("Updated");
        final Date updatedBirthDate = Date.valueOf("2001-01-01");
        foundPatient.setBirthDate(updatedBirthDate);
        foundPatient.setGender(Enumerations.AdministrativeGender.UNKNOWN);

        // Update the patient

        final MethodOutcome updated = client
                .update()
                .resource(foundPatient)
                .encodedJson()
                .execute();

        final Patient updatedPatient = (Patient) updated.getResource();

        foundPatient.getNameFirstRep().setFamily("<script>Family</script>");
        assertThrows(InvalidRequestException.class, () -> client
                .update()
                .resource(foundPatient)
                .encodedJson()
                .execute(), "Should not have updated patient");

        // Try to pull the record, again, from the DB

        final Patient fetchedPatient = client
                .read()
                .resource(Patient.class)
                .withId(foundPatient.getId())
                .encodedJson()
                .execute();

        final java.util.Date lastUpdated = fetchedPatient.getMeta().getLastUpdated();
        fetchedPatient.setMeta(null);
        updatedPatient.setMeta(null);

        assertAll(() -> assertTrue(fetchedPatient.equalsDeep(updatedPatient), "Should match updated record"),
                () -> assertEquals("Updated", fetchedPatient.getNameFirstRep().getFamily(), "Should have updated family name"),
                () -> assertTrue(createdAt.before(lastUpdated), "Update timestamp should be later"));
    }

    @Test
    void testPatientUpdateWithInvalidMbi() {
        final IGenericClient client = createFHIRClient(ctx, getServerURL());
        final String mbi = "4S41C00AA00";

        final Bundle result = client
                .search()
                .forResource(Patient.class)
                .where(Patient.IDENTIFIER.exactly().systemAndCode(DPCIdentifierSystem.MBI.getSystem(), mbi))
                .and(Patient.ORGANIZATION.hasId("Organization/" + DEFAULT_ORG_ID))
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        final Patient foundPatient = (Patient) result.getEntryFirstRep().getResource();
        foundPatient.getIdentifierFirstRep().setValue("not-a-valid-MBI");
        IUpdateTyped update = client
                .update()
                .resource(foundPatient);

        assertThrows(UnprocessableEntityException.class, update::execute);
    }
}
