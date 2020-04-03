package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IOperationUntypedWithInput;
import ca.uhn.fhir.rest.gclient.IReadExecutable;
import ca.uhn.fhir.rest.gclient.IUpdateExecutable;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import gov.cms.dpc.api.APITestHelpers;
import gov.cms.dpc.api.AbstractSecureApplicationTest;
import gov.cms.dpc.common.utils.SeedProcessor;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.fhir.helpers.FHIRHelpers;
import gov.cms.dpc.testing.APIAuthHelpers;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.*;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.sql.Date;
import java.util.List;
import java.util.UUID;

import static gov.cms.dpc.api.APITestHelpers.ORGANIZATION_ID;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PatientResourceTest extends AbstractSecureApplicationTest {

    public static final String PROVENANCE_FMT = "{ \"resourceType\": \"Provenance\", \"recorded\": \"" + DateTimeType.now().getValueAsString() + "\"," +
            " \"reason\": [ { \"system\": \"http://hl7.org/fhir/v3/ActReason\", \"code\": \"TREAT\"  } ], \"agent\": [ { \"role\": " +
            "[ { \"coding\": [ { \"system\":" + "\"http://hl7.org/fhir/v3/RoleClass\", \"code\": \"AGNT\" } ] } ], \"whoReference\": " +
            "{ \"reference\": \"Organization/ORGANIZATION_ID\" }, \"onBehalfOfReference\": { \"reference\": " +
            "\"Practitioner/PRACTITIONER_ID\" } } ] }";

    PatientResourceTest() {
        // Not used
    }

    @Test
    @Order(1)
    void ensurePatientsExist() throws IOException, URISyntaxException, NoSuchAlgorithmException {
        final IParser parser = ctx.newJsonParser();
        final IGenericClient attrClient = APITestHelpers.buildAttributionClient(ctx);
        IGenericClient client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY);
        APITestHelpers.setupPatientTest(client, parser);

        final Bundle patients = client
                .search()
                .forResource(Patient.class)
                .encodedJson()
                .returnBundle(Bundle.class)
                .execute();

        assertEquals(100, patients.getTotal(), "Should have correct number of patients");

        final Bundle specificSearch = client
                .search()
                .forResource(Patient.class)
                .where(Patient.IDENTIFIER.exactly().systemAndCode(DPCIdentifierSystem.MBI.getSystem(), "4S41C00AA00"))
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        assertEquals(1, specificSearch.getTotal(), "Should have a single patient");

        // Fetch the patient directly
        final Patient foundPatient = (Patient) specificSearch.getEntryFirstRep().getResource();

        final Patient queriedPatient = client
                .read()
                .resource(Patient.class)
                .withId(foundPatient.getIdElement())
                .encodedJson()
                .execute();

        assertTrue(foundPatient.equalsDeep(queriedPatient), "Search and GET should be identical");

        // Create a new org and make sure it has no providers
        final String m2 = FHIRHelpers.registerOrganization(attrClient, parser, OTHER_ORG_ID, getAdminURL());
        // Submit a new public key to use for JWT flow
        final String keyID = "new-key";
        final Pair<UUID, PrivateKey> uuidPrivateKeyPair = APIAuthHelpers.generateAndUploadKey(keyID, OTHER_ORG_ID, GOLDEN_MACAROON, getBaseURL());

        // Update the authenticated client to use the new organization
        client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), m2, uuidPrivateKeyPair.getLeft(), uuidPrivateKeyPair.getRight());

        final Bundle otherPatients = client
                .search()
                .forResource(Patient.class)
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        assertEquals(0, otherPatients.getTotal(), "Should not have any practitioners");

        // Try to look for one of the other patients
        final IReadExecutable<Patient> fetchRequest = client
                .read()
                .resource(Patient.class)
                .withId(foundPatient.getId())
                .encodedJson();

        assertThrows(AuthenticationException.class, fetchRequest::execute, "Should not be authorized");

        // Search, and find nothing
        final Bundle otherSpecificSearch = client
                .search()
                .forResource(Patient.class)
                .where(Patient.IDENTIFIER.exactly().identifier(foundPatient.getIdentifierFirstRep().getValue()))
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        assertEquals(0, otherSpecificSearch.getTotal(), "Should have a specific provider");
    }

    @Test
    @Order(2)
    void testPatientRemoval() throws IOException, URISyntaxException, NoSuchAlgorithmException {
        final IParser parser = ctx.newJsonParser();
        final IGenericClient attrClient = APITestHelpers.buildAttributionClient(ctx);
        final String macaroon = FHIRHelpers.registerOrganization(attrClient, parser, ORGANIZATION_ID, getAdminURL());
        final String keyLabel = "patient-deletion-key";
        final Pair<UUID, PrivateKey> uuidPrivateKeyPair = APIAuthHelpers.generateAndUploadKey(keyLabel, ORGANIZATION_ID, GOLDEN_MACAROON, getBaseURL());
        final IGenericClient client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), macaroon, uuidPrivateKeyPair.getLeft(), uuidPrivateKeyPair.getRight());

        final Bundle patients = client
                .search()
                .forResource(Patient.class)
                .encodedJson()
                .returnBundle(Bundle.class)
                .execute();

        assertEquals(100, patients.getTotal(), "Should have correct number of patients");

        // Try to remove one

        final Patient patient = (Patient) patients.getEntry().get(patients.getTotal() - 2).getResource();

        client
                .delete()
                .resource(patient)
                .encodedJson()
                .execute();

        // Make sure it's done

        final IReadExecutable<Patient> fetchRequest = client
                .read()
                .resource(Patient.class)
                .withId(patient.getId())
                .encodedJson();

        // TODO: DPC-433, this really should be NotFound, but we can't disambiguate between the two cases
        assertThrows(AuthenticationException.class, fetchRequest::execute, "Should not have found the resource");

        // Search again
        final Bundle secondSearch = client
                .search()
                .forResource(Patient.class)
                .encodedJson()
                .returnBundle(Bundle.class)
                .execute();

        assertEquals(99, secondSearch.getTotal(), "Should have correct number of patients");
    }

    @Test
    @Order(3)
    void testPatientUpdating() throws IOException, URISyntaxException, NoSuchAlgorithmException {
        final IParser parser = ctx.newJsonParser();
        final IGenericClient attrClient = APITestHelpers.buildAttributionClient(ctx);
        final String macaroon = FHIRHelpers.registerOrganization(attrClient, parser, ORGANIZATION_ID, getAdminURL());
        final String keyLabel = "patient-update-key";
        final Pair<UUID, PrivateKey> uuidPrivateKeyPair = APIAuthHelpers.generateAndUploadKey(keyLabel, ORGANIZATION_ID, GOLDEN_MACAROON, getBaseURL());
        final IGenericClient client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), macaroon, uuidPrivateKeyPair.getLeft(), uuidPrivateKeyPair.getRight());

        final Bundle patients = client
                .search()
                .forResource(Patient.class)
                .encodedJson()
                .returnBundle(Bundle.class)
                .execute();

        assertEquals(99, patients.getTotal(), "Should have correct number of patients");

        // Try to update one
        final Patient patient = (Patient) patients.getEntry().get(patients.getTotal() - 2).getResource();
        patient.setBirthDate(Date.valueOf("2000-01-01"));
        patient.setGender(Enumerations.AdministrativeGender.MALE);

        final MethodOutcome outcome = client
                .update()
                .resource(patient)
                .withId(patient.getId())
                .encodedJson()
                .execute();

        assertTrue(((Patient) outcome.getResource()).equalsDeep(patient), "Should have been updated correctly");

        // Try to update with invalid MBI
        Identifier mbiIdentifier = patient.getIdentifier().stream()
                .filter(i -> DPCIdentifierSystem.MBI.getSystem().equals(i.getSystem())).findFirst().get();
        mbiIdentifier.setValue("not-a-valid-MBI");

        IUpdateExecutable update = client
                .update()
                .resource(patient)
                .withId(patient.getId());

        assertThrows(UnprocessableEntityException.class, update::execute);
    }

    @Test
    @Order(4)
    void testPatientEverything() throws IOException, URISyntaxException, NoSuchAlgorithmException {
        final IParser parser = ctx.newJsonParser();
        final IGenericClient attrClient = APITestHelpers.buildAttributionClient(ctx);
        String macaroon = FHIRHelpers.registerOrganization(attrClient, parser, ORGANIZATION_ID, getAdminURL());
        String keyLabel = "patient-everything-key";
        Pair<UUID, PrivateKey> uuidPrivateKeyPair = APIAuthHelpers.generateAndUploadKey(keyLabel, ORGANIZATION_ID, GOLDEN_MACAROON, getBaseURL());
        IGenericClient client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), macaroon, uuidPrivateKeyPair.getLeft(), uuidPrivateKeyPair.getRight(), false, true);
        APITestHelpers.setupPractitionerTest(client, parser);

        final Bundle patients = client
                .search()
                .forResource(Patient.class)
                .encodedJson()
                .returnBundle(Bundle.class)
                .execute();
        final Patient patient = (Patient) patients.getEntry().get(patients.getTotal() - 17).getResource();
        final String patientId = FHIRExtractors.getEntityUUID(patient.getId()).toString();

        Bundle practSearch = client
                .search()
                .forResource(Practitioner.class)
                .where(Practitioner.IDENTIFIER.exactly().code("8075963174210588464"))
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();
        Practitioner foundProvider = (Practitioner) practSearch.getEntryFirstRep().getResource();

        Group group = SeedProcessor.createBaseAttributionGroup(FHIRExtractors.getProviderNPI(foundProvider), ORGANIZATION_ID);
        final Reference patientRef = new Reference("Patient/" + patientId);
        group.addMember().setEntity(patientRef);

        String provenance = PROVENANCE_FMT.replaceAll("ORGANIZATION_ID", ORGANIZATION_ID).replace("PRACTITIONER_ID", foundProvider.getId());
        client
                .create()
                .resource(group)
                .withAdditionalHeader("X-Provenance", provenance)
                .encodedJson()
                .execute();

        Bundle result = client
                .operation()
                .onInstance(new IdType("Patient", patientId))
                .named("$everything")
                .withNoParameters(Parameters.class)
                .returnResourceType(Bundle.class)
                .useHttpGet()
                .withAdditionalHeader("X-Provenance", provenance)
                .execute();

        assertEquals(31, result.getTotal(), "Should have 31 entries in Bundle");
        for (Bundle.BundleEntryComponent bec : result.getEntry()) {
            List<ResourceType> resourceTypes = List.of(ResourceType.Coverage, ResourceType.ExplanationOfBenefit, ResourceType.Patient);
            assertTrue(resourceTypes.contains(bec.getResource().getResourceType()), "Resource type should be Coverage, EOB, or Patient");
        }

        // With unattributed provider in X-Provenance
        macaroon = FHIRHelpers.registerOrganization(attrClient, parser, OTHER_ORG_ID, getAdminURL());
        keyLabel = "patient-everything-key-2";
        uuidPrivateKeyPair = APIAuthHelpers.generateAndUploadKey(keyLabel, OTHER_ORG_ID, GOLDEN_MACAROON, getBaseURL());
        client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), macaroon, uuidPrivateKeyPair.getLeft(), uuidPrivateKeyPair.getRight());
        APITestHelpers.setupPractitionerTest(client, parser);

        Bundle practitioners = client
                .search()
                .forResource(Practitioner.class)
                .where(Practitioner.IDENTIFIER.exactly().code("164333597980511237"))
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();
        Practitioner provider = (Practitioner) practitioners.getEntryFirstRep().getResource();

        group = SeedProcessor.createBaseAttributionGroup(FHIRExtractors.getProviderNPI(provider), OTHER_ORG_ID);
        final Patient otherPatient = (Patient) patients.getEntry().get(patients.getTotal() - 18).getResource();
        Reference otherPatientRef = new Reference("Patient/" + otherPatient.getId());
        group.addMember().setEntity(otherPatientRef);
        provenance = PROVENANCE_FMT.replaceAll("ORGANIZATION_ID", OTHER_ORG_ID).replace("PRACTITIONER_ID", provider.getId());
        client
                .create()
                .resource(group)
                .withAdditionalHeader("X-Provenance", provenance)
                .encodedJson()
                .execute();

        IOperationUntypedWithInput everythingOp = client
                .operation()
                .onInstance(new IdType("Patient", patientId))
                .named("$everything")
                .withNoParameters(Parameters.class)
                .returnResourceType(Bundle.class)
                .useHttpGet()
                .withAdditionalHeader("X-Provenance", provenance);

        assertThrows(AuthenticationException.class, everythingOp::execute);
    }
}
