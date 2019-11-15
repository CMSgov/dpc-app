package gov.cms.dpc.attribution.resources;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICreateTyped;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import gov.cms.dpc.attribution.AbstractAttributionTest;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.util.UUID;

import static gov.cms.dpc.attribution.AttributionTestHelpers.*;
import static org.junit.jupiter.api.Assertions.*;

class PatientResourceTest extends AbstractAttributionTest {

    private PatientResourceTest() {
        // Not used
    }

    @Test
    void testPatientReadWrite() {
        final Patient patient = createPatientResource("1871", DEFAULT_ORG_ID);

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
    void testPatientSearch() {
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

        // Try for wrong org

        final Bundle secondSearch = client
                .search()
                .forResource(Patient.class)
                .where(Patient.IDENTIFIER.exactly().systemAndCode(DPCIdentifierSystem.MBI.getSystem(), DEFAULT_PATIENT_MBI))
                .and(Patient.ORGANIZATION.hasId("Organization/" + UUID.randomUUID().toString()))
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        assertEquals(0, secondSearch.getTotal(), "Should not have any patients");
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

        // Remove the patient and try the search again
        final Patient patient = (Patient) firstSearch.getEntryFirstRep().getResource();

        final IBaseOperationOutcome outcome = client
                .delete()
                .resource(patient)
                .encodedJson()
                .execute();


        assertNull(outcome, "Should have succeeded with empty outcome");

        // Try the first search again, which should be empty

        final Bundle finalSearch = firstQuery.execute();
        assertEquals(0, finalSearch.getTotal(), "Should not have any patients");
    }

    @Test
    void testPatientUpdate() {
        final IGenericClient client = createFHIRClient(ctx, getServerURL());

        final IQuery<Bundle> firstQuery = client
                .search()
                .forResource(Patient.class)
                .where(Patient.IDENTIFIER.exactly().systemAndCode(DPCIdentifierSystem.MBI.getSystem(), "19990000002902"))
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
}