package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICreateTyped;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import gov.cms.dpc.api.APITestHelpers;
import gov.cms.dpc.api.AbstractSecureApplicationTest;
import gov.cms.dpc.common.utils.SeedProcessor;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.testing.APIAuthHelpers;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.dstu3.model.codesystems.V3RoleClass;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.sql.Date;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;

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
                .where(Practitioner.IDENTIFIER.exactly().code("8075963174210588464"))
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
}
