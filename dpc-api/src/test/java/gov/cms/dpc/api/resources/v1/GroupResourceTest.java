package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICreateTyped;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import gov.cms.dpc.api.APITestHelpers;
import gov.cms.dpc.api.AbstractSecureApplicationTest;
import gov.cms.dpc.common.utils.NPIUtil;
import gov.cms.dpc.common.utils.SeedProcessor;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.testing.APIAuthHelpers;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.apache.http.HttpHeaders;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.dstu3.model.codesystems.V3RoleClass;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.HttpMethod;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static gov.cms.dpc.api.APITestHelpers.ORGANIZATION_ID;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(BufferedLoggerHandler.class)
public class GroupResourceTest extends AbstractSecureApplicationTest {

    GroupResourceTest() {
        // Not used
    }

    @Test
    void testMissingProvenance() throws IOException {
        final IParser parser = ctx.newJsonParser();
        IGenericClient client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY);
        APITestHelpers.setupPatientTest(client, parser);
        APITestHelpers.setupPractitionerTest(client, parser);

        // Create a patient
        final Bundle specificSearch = client
                .search()
                .forResource(Patient.class)
                .where(Patient.IDENTIFIER.exactly().systemAndCode(DPCIdentifierSystem.MBI.getSystem(), "4S41C00AA00"))
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        assertEquals(1, specificSearch.getTotal(), "Should have a single patient");

        final Patient patient = (Patient) specificSearch.getEntryFirstRep().getResource();

        // Create the practitioner
        final Bundle practSearch = client
                .search()
                .forResource(Practitioner.class)
                .where(Practitioner.IDENTIFIER.exactly().code("1232131239"))
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        assertEquals(1, practSearch.getTotal(), "Should have a specific provider");

        // Fetch the provider directly
        final Practitioner foundProvider = (Practitioner) practSearch.getEntryFirstRep().getResource();


        final Group group = SeedProcessor.createBaseAttributionGroup(FHIRExtractors.getProviderNPI(foundProvider), ORGANIZATION_ID);

        final Reference patientRef = new Reference(patient.getIdElement());
        group.addMember().setEntity(patientRef);

        // Submit the group

        final ICreateTyped creation = client
                .create()
                .resource(group)
                .encodedJson();

        final InvalidRequestException invalidRequestException = assertThrows(InvalidRequestException.class, creation::execute, "Should throw a 400");
        final OperationOutcome operationOutcome = (OperationOutcome) invalidRequestException.getOperationOutcome();
        assertAll(() -> assertEquals(HttpStatus.BAD_REQUEST_400, invalidRequestException.getStatusCode(), "Should have 400 status"),
                () -> assertEquals("Must have X-Provenance header", operationOutcome.getIssueFirstRep().getDetails().getText(), "Should have correct message"));

        // Try again with provenance
        final Provenance provenance = new Provenance();
        provenance.setRecorded(Date.valueOf(Instant.now().atZone(ZoneOffset.UTC).toLocalDate()));
        final Coding coding = new Coding();
        coding.setSystem("http://hl7.org/fhir/v3/ActReason");
        coding.setCode("TREAT");
        provenance.setReason(Collections.singletonList(coding));
        provenance.setTarget(Collections.singletonList(patientRef));
        final Provenance.ProvenanceAgentComponent component = new Provenance.ProvenanceAgentComponent();

        final Coding roleCode = new Coding();
        roleCode.setSystem(V3RoleClass.AGNT.getSystem());
        roleCode.setCode(V3RoleClass.AGNT.toCode());

        final CodeableConcept roleConcept = new CodeableConcept();
        roleConcept.addCoding(roleCode);
        component.setRole(Collections.singletonList(roleConcept));
        component.setWho(new Reference(new IdType("Organization", ORGANIZATION_ID)));
        component
                .setOnBehalfOf(new Reference(foundProvider.getIdElement()));

        provenance.addAgent(component);

        creation
                .withAdditionalHeader("X-Provenance", ctx.newJsonParser().encodeResourceToString(provenance));

        creation.execute();
    }

    @Test
    public void testInvalidInputsWhenCreatingGroup() throws IOException {
        final IParser parser = ctx.newJsonParser();
        IGenericClient client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY);
        APITestHelpers.setupPatientTest(client, parser);
        APITestHelpers.setupPractitionerTest(client, parser);

        // Grab the created patient
        final Bundle specificSearch = client
                .search()
                .forResource(Patient.class)
                .where(Patient.IDENTIFIER.exactly().systemAndCode(DPCIdentifierSystem.MBI.getSystem(), "4S41C00AA00"))
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        assertEquals(1, specificSearch.getTotal(), "Should have a single patient");

        final Patient patient = (Patient) specificSearch.getEntryFirstRep().getResource();

        // Grab the created practitioner
        final Bundle practSearch = client
                .search()
                .forResource(Practitioner.class)
                .where(Practitioner.IDENTIFIER.exactly().code("1234329724"))
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        assertEquals(1, practSearch.getTotal(), "Should have a specific provider");

        // Fetch the provider directly
        final Practitioner foundProvider = (Practitioner) practSearch.getEntryFirstRep().getResource();

        final Group group = SeedProcessor.createBaseAttributionGroup(FHIRExtractors.getProviderNPI(foundProvider), ORGANIZATION_ID);

        final Reference patientRef = new Reference(patient.getIdElement());
        group.addMember().setEntity(patientRef);

        String safeCodeValue = group.getCharacteristicFirstRep().getCode().getCodingFirstRep().getCode();
        group.getCharacteristicFirstRep().getCode().getCodingFirstRep().setCode("<script>nope</script>");

        // Submit the group
        assertThrows(InvalidRequestException.class, () -> client
                .create()
                .resource(group)
                .encodedJson()
                .execute());

        // Reset back to good safe value
        group.getCharacteristicFirstRep().getCode().getCodingFirstRep().setCode(safeCodeValue);

        // Test scripts in provenance
        final Provenance provenance = new Provenance();
        provenance.setRecorded(Date.valueOf(Instant.now().atZone(ZoneOffset.UTC).toLocalDate()));
        final Coding coding = new Coding();
        coding.setSystem("http://hl7.org/fhir/v3/ActReason");
        coding.setCode("<script>nope</script>");
        provenance.setReason(Collections.singletonList(coding));
        provenance.setTarget(Collections.singletonList(patientRef));
        final Provenance.ProvenanceAgentComponent component = new Provenance.ProvenanceAgentComponent();

        final Coding roleCode = new Coding();
        roleCode.setSystem(V3RoleClass.AGNT.getSystem());
        roleCode.setCode(V3RoleClass.AGNT.toCode());

        final CodeableConcept roleConcept = new CodeableConcept();
        roleConcept.addCoding(roleCode);
        component.setRole(Collections.singletonList(roleConcept));
        component.setWho(new Reference(new IdType("Organization", ORGANIZATION_ID)));
        component
                .setOnBehalfOf(new Reference(foundProvider.getIdElement()));

        provenance.addAgent(component);

        //Provenance parser can't parse
        assertThrows(UnprocessableEntityException.class, () -> client
                .create()
                .resource(group)
                .encodedJson()
                .withAdditionalHeader("X-Provenance", ctx.newJsonParser().encodeResourceToString(provenance))
                .execute());
    }

    @Test
    void testCreateInvalidGroup() throws IOException, URISyntaxException {
        Provenance provenance = new Provenance();
        provenance.setRecorded(Date.valueOf(LocalDate.now()));
        provenance.setReason(List.of(new Coding("http://hl7.org/fhir/v3/ActReason", "TREAT", null)));
        String provString = ctx.newJsonParser().encodeResourceToString(provenance);

        URL url = new URL(getBaseURL() + "/Group");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(HttpMethod.POST);
        conn.setRequestProperty(HttpHeaders.CONTENT_TYPE, "application/fhir+json");
        conn.setRequestProperty("X-Provenance", provString);

        APIAuthHelpers.AuthResponse auth = APIAuthHelpers.jwtAuthFlow(getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY);
        conn.setRequestProperty(HttpHeaders.AUTHORIZATION, "Bearer " + auth.accessToken);

        conn.setDoOutput(true);
        String reqBody = "{\"test\": \"test\"}";
        conn.getOutputStream().write(reqBody.getBytes());

        assertEquals(HttpStatus.BAD_REQUEST_400, conn.getResponseCode());

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
            StringBuilder respBuilder = new StringBuilder();
            String respLine = null;
            while ((respLine = reader.readLine()) != null) {
                respBuilder.append(respLine.trim());
            }
            String resp = respBuilder.toString();
            assertAll(() -> assertTrue(resp.contains("\"resourceType\":\"OperationOutcome\"")),
                    () -> assertTrue(resp.contains("Invalid JSON content")));
        }

        conn.disconnect();
    }

    @Test
    public void testCreateGroupReturnsAppropriateHeaders() throws IOException {
        final IParser parser = ctx.newJsonParser();
        IGenericClient client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY);
        APITestHelpers.setupPatientTest(client, parser);
        APITestHelpers.setupPractitionerTest(client, parser);

        // Grab the created patient
        final Bundle specificSearch = client
                .search()
                .forResource(Patient.class)
                .where(Patient.IDENTIFIER.exactly().systemAndCode(DPCIdentifierSystem.MBI.getSystem(), "4S41C00AA00"))
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        assertEquals(1, specificSearch.getTotal(), "Should have a single patient");

        final Patient patient = (Patient) specificSearch.getEntryFirstRep().getResource();

        // Grab the created practitioner
        final Bundle practSearch = client
                .search()
                .forResource(Practitioner.class)
                .where(Practitioner.IDENTIFIER.exactly().code("1234329724"))
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        assertEquals(1, practSearch.getTotal(), "Should have a specific provider");

        // Fetch the provider directly
        final Practitioner foundProvider = (Practitioner) practSearch.getEntryFirstRep().getResource();

        final Group group = SeedProcessor.createBaseAttributionGroup(FHIRExtractors.getProviderNPI(foundProvider), ORGANIZATION_ID);

        final Reference patientRef = new Reference(patient.getIdElement());
        group.addMember().setEntity(patientRef);


        // Test scripts in provenance
        final Provenance provenance = new Provenance();
        provenance.setRecorded(Date.valueOf(Instant.now().atZone(ZoneOffset.UTC).toLocalDate()));
        final Coding coding = new Coding();
        coding.setSystem("http://hl7.org/fhir/v3/ActReason");
        coding.setCode("TREAT");
        provenance.setReason(Collections.singletonList(coding));
        provenance.setTarget(Collections.singletonList(patientRef));
        final Provenance.ProvenanceAgentComponent component = new Provenance.ProvenanceAgentComponent();

        final Coding roleCode = new Coding();
        roleCode.setSystem(V3RoleClass.AGNT.getSystem());
        roleCode.setCode(V3RoleClass.AGNT.toCode());

        final CodeableConcept roleConcept = new CodeableConcept();
        roleConcept.addCoding(roleCode);
        component.setRole(Collections.singletonList(roleConcept));
        component.setWho(new Reference(new IdType("Organization", ORGANIZATION_ID)));
        component
                .setOnBehalfOf(new Reference(foundProvider.getIdElement()));

        provenance.addAgent(component);

        MethodOutcome methodOutcome = client
                .create()
                .resource(group)
                .encodedJson()
                .withAdditionalHeader("X-Provenance", ctx.newJsonParser().encodeResourceToString(provenance))
                .execute();

        String location = methodOutcome.getResponseHeaders().get("location").get(0);
        assertNotNull(location);

        Group foundGroup = client.read()
                .resource(Group.class)
                .withUrl(location)
                .encodedJson()
                .execute();

        assertEquals(group.getIdentifierFirstRep().getValue(), foundGroup.getIdentifierFirstRep().getValue());

        client.delete()
                .resource(foundGroup)
                .encodedJson()
                .execute();
    }

    @Test
    public void testProvenanceHeaderAndGroupProviderMatch() {
        IGenericClient client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY);
        Practitioner practitioner = APITestHelpers.createPractitionerResource(NPIUtil.generateNPI(),ORGANIZATION_ID);

        MethodOutcome methodOutcome = client.create()
                .resource(practitioner)
                .encodedJson()
                .execute();

        Practitioner createdPractitioner = (Practitioner) methodOutcome.getResource();

        //Group will have non-matching practitioner NPI
        Group group = SeedProcessor.createBaseAttributionGroup(NPIUtil.generateNPI(), ORGANIZATION_ID);

        final Provenance provenance = new Provenance();
        provenance.setRecorded(Date.valueOf(Instant.now().atZone(ZoneOffset.UTC).toLocalDate()));
        final Coding coding = new Coding();
        coding.setSystem("http://hl7.org/fhir/v3/ActReason");
        coding.setCode("TREAT");
        provenance.setReason(Collections.singletonList(coding));
        final Provenance.ProvenanceAgentComponent component = new Provenance.ProvenanceAgentComponent();

        final Coding roleCode = new Coding();
        roleCode.setSystem(V3RoleClass.AGNT.getSystem());
        roleCode.setCode(V3RoleClass.AGNT.toCode());

        final CodeableConcept roleConcept = new CodeableConcept();
        roleConcept.addCoding(roleCode);
        component.setRole(Collections.singletonList(roleConcept));
        component.setWho(new Reference(new IdType("Organization", ORGANIZATION_ID)));
        component.setOnBehalfOf(new Reference(createdPractitioner.getIdElement()));

        provenance.addAgent(component);

        ICreateTyped createGroup = client
                .create()
                .resource(group)
                .encodedJson()
                .withAdditionalHeader("X-Provenance", ctx.newJsonParser().encodeResourceToString(provenance));

        Assertions.assertThrows(UnprocessableEntityException.class, createGroup::execute);

        group = SeedProcessor.createBaseAttributionGroup(FHIRExtractors.getProviderNPI(createdPractitioner), ORGANIZATION_ID);
        //set provenance practitioner to unknown practitioner;
        component.setOnBehalfOf(new Reference(new IdType("Practitioner", UUID.randomUUID().toString())));

        createGroup = client
                .create()
                .resource(group)
                .encodedJson()
                .withAdditionalHeader("X-Provenance", ctx.newJsonParser().encodeResourceToString(provenance));

        Assertions.assertThrows(UnprocessableEntityException.class, createGroup::execute);

        client.delete()
                .resource(createdPractitioner)
                .encodedJson()
                .execute();
    }
}
