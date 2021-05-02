package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICreateTyped;
import ca.uhn.fhir.rest.gclient.IOperationUntypedWithInput;
import ca.uhn.fhir.rest.gclient.IReadExecutable;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.google.common.net.HttpHeaders;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.bluebutton.client.BlueButtonClient;
import gov.cms.dpc.common.utils.NPIUtil;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.FHIRMediaTypes;
import gov.cms.dpc.queue.IJobQueue;
import org.hl7.fhir.dstu3.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import static gov.cms.dpc.api.resources.v1.GroupResource.SYNTHETIC_BENE_ID;
import static gov.cms.dpc.fhir.FHIRMediaTypes.FHIR_JSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
public class GroupResourceUnitTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    IGenericClient attributionClient;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    BlueButtonClient mockBfdClient;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    IJobQueue mockQueue;

    @Mock
    HttpServletRequest request;

    GroupResource resource;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        resource = new GroupResource(mockQueue, attributionClient, "http://localhost:3002/v1", mockBfdClient);
    }

    @Test
    public void testCreateRoster() {
        String practitionerNPI = NPIUtil.generateNPI();
        UUID practitionerId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        Organization organization = new Organization();
        organization.setId(orgId.toString());
        OrganizationPrincipal organizationPrincipal = new OrganizationPrincipal(organization);

        Provenance provenance = new Provenance();
        Provenance.ProvenanceAgentComponent provenanceAgent = new Provenance.ProvenanceAgentComponent();
        provenanceAgent.addRole().addCoding().setCode("AGNT");
        provenanceAgent.setWho(new Reference("Organization/" + UUID.randomUUID()));
        provenanceAgent.setOnBehalfOf(new Reference("Practitioner/" + practitionerId));
        provenance.addAgent(provenanceAgent);

        Group group = new Group();
        group.addCharacteristic().getCode().addCoding().setCode("attributed-to");
        CodeableConcept codeableConcept = new CodeableConcept();
        codeableConcept.addCoding().setSystem(DPCIdentifierSystem.NPPES.getSystem()).setCode(practitionerNPI);
        group.getCharacteristicFirstRep().setValue(codeableConcept);

        ICreateTyped createExec = mock(ICreateTyped.class);
        when(attributionClient.create().resource(group).encodedJson()).thenReturn(createExec);

        MethodOutcome outcome = new MethodOutcome();
        outcome.setResource(group);
        when(createExec.execute()).thenReturn(outcome);

        Identifier identifier = new Identifier();
        identifier.setSystem(DPCIdentifierSystem.NPPES.getSystem());
        identifier.setValue(practitionerNPI);
        Practitioner practitioner = new Practitioner();
        practitioner.setIdentifier(List.of(identifier));
        IReadExecutable<Practitioner> readExec = mock(IReadExecutable.class);
        when(attributionClient.read().resource(Practitioner.class).withId(practitionerId.toString()).encodedJson()).thenReturn(readExec);
        when(readExec.execute()).thenReturn(practitioner);

        Response response = resource.createRoster(organizationPrincipal, provenance, group);
        Group result = (Group) response.getEntity();

        assertEquals("Organization ID", result.getMeta().getTag(DPCIdentifierSystem.DPC.getSystem(), orgId.toString()).getDisplay());
        assertEquals(group, result);
    }

    @Test
    public void testCreateRosterNonMatchingNPI() {
        UUID practitionerId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        Organization organization = new Organization();
        organization.setId(orgId.toString());
        OrganizationPrincipal organizationPrincipal = new OrganizationPrincipal(organization);

        Provenance provenance = new Provenance();
        Provenance.ProvenanceAgentComponent provenanceAgent = new Provenance.ProvenanceAgentComponent();
        provenanceAgent.addRole().addCoding().setCode("AGNT");
        provenanceAgent.setWho(new Reference("Organization/" + UUID.randomUUID()));
        provenanceAgent.setOnBehalfOf(new Reference("Practitioner/" + practitionerId));
        provenance.addAgent(provenanceAgent);

        Group group = new Group();
        group.addCharacteristic().getCode().addCoding().setCode("attributed-to");
        CodeableConcept codeableConcept = new CodeableConcept();
        codeableConcept.addCoding().setSystem(DPCIdentifierSystem.NPPES.getSystem()).setCode(NPIUtil.generateNPI());
        group.getCharacteristicFirstRep().setValue(codeableConcept);

        ICreateTyped createExec = mock(ICreateTyped.class);
        when(attributionClient.create().resource(group).encodedJson()).thenReturn(createExec);

        MethodOutcome outcome = new MethodOutcome();
        outcome.setResource(group);
        when(createExec.execute()).thenReturn(outcome);

        Identifier identifier = new Identifier();
        identifier.setSystem(DPCIdentifierSystem.NPPES.getSystem());
        identifier.setValue(NPIUtil.generateNPI());
        Practitioner practitioner = new Practitioner();
        practitioner.setIdentifier(List.of(identifier));
        IReadExecutable<Practitioner> readExec = mock(IReadExecutable.class);
        when(attributionClient.read().resource(Practitioner.class).withId(practitionerId.toString()).encodedJson()).thenReturn(readExec);
        when(readExec.execute()).thenReturn(practitioner);

        Assertions.assertThrows(WebApplicationException.class, () -> resource.createRoster(organizationPrincipal, provenance, group));
    }

    @Test
    public void testCreateRosterProviderNotFound() {
        UUID practitionerId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        Organization organization = new Organization();
        organization.setId(orgId.toString());
        OrganizationPrincipal organizationPrincipal = new OrganizationPrincipal(organization);

        Provenance provenance = new Provenance();
        Provenance.ProvenanceAgentComponent provenanceAgent = new Provenance.ProvenanceAgentComponent();
        provenanceAgent.addRole().addCoding().setCode("AGNT");
        provenanceAgent.setWho(new Reference("Organization/" + UUID.randomUUID()));
        provenanceAgent.setOnBehalfOf(new Reference("Practitioner/" + practitionerId));
        provenance.addAgent(provenanceAgent);

        Group group = new Group();
        group.addCharacteristic().getCode().addCoding().setCode("attributed-to");
        CodeableConcept codeableConcept = new CodeableConcept();
        codeableConcept.addCoding().setSystem(DPCIdentifierSystem.NPPES.getSystem()).setCode(NPIUtil.generateNPI());
        group.getCharacteristicFirstRep().setValue(codeableConcept);

        ICreateTyped createExec = mock(ICreateTyped.class);
        when(attributionClient.create().resource(group).encodedJson()).thenReturn(createExec);

        MethodOutcome outcome = new MethodOutcome();
        outcome.setResource(group);
        when(createExec.execute()).thenReturn(outcome);

        IReadExecutable<Practitioner> readExec = mock(IReadExecutable.class);
        when(attributionClient.read().resource(Practitioner.class).withId(practitionerId.toString()).encodedJson()).thenReturn(readExec);
        when(readExec.execute()).thenThrow(new ResourceNotFoundException("practitioner not found"));

        Assertions.assertThrows(WebApplicationException.class, () -> resource.createRoster(organizationPrincipal, provenance, group));
    }

    @Test
    public void testExportWithValidSinceParam() {
        UUID orgId = UUID.randomUUID();
        Organization organization = new Organization();
        organization.setId(orgId.toString());
        Identifier identifier = new Identifier();
        identifier.setSystem(DPCIdentifierSystem.NPPES.getSystem()).setValue(NPIUtil.generateNPI());
        organization.setIdentifier(List.of(identifier));
        OrganizationPrincipal organizationPrincipal = new OrganizationPrincipal(organization);

        String groupId = "123456789";

        //Mock Group
        Group group = new Group();
        group.setId(groupId);
        group.addMember();
        group.addCharacteristic().getCode().addCoding().setCode("attributed-to");
        CodeableConcept codeableConcept = new CodeableConcept();
        codeableConcept.addCoding().setSystem(DPCIdentifierSystem.NPPES.getSystem()).setCode(NPIUtil.generateNPI());
        group.getCharacteristicFirstRep().setValue(codeableConcept);

        IReadExecutable<Group> readExec = mock(IReadExecutable.class);
        when(attributionClient.read().resource(Group.class).withId(new IdType("Group", groupId)).encodedJson()).thenReturn(readExec);
        when(readExec.execute()).thenReturn(group);

        IReadExecutable<Organization> readExec2 = mock(IReadExecutable.class);
        when(attributionClient.read().resource(Organization.class).withId(new IdType("Organization", orgId.toString())).encodedJson()).thenReturn(readExec2);
        when(readExec2.execute()).thenReturn(organization);

        //Mock get bundle
        IOperationUntypedWithInput<Bundle> bundleOperation = mock(IOperationUntypedWithInput.class);
        when(attributionClient
                .operation()
                .onInstance(new IdType(groupId))
                .named("patients")
                .withParameters(any(Parameters.class))
                .returnResourceType(Bundle.class)
                .useHttpGet()
                .encodedJson()).thenReturn(bundleOperation);

        when(bundleOperation.execute()).thenReturn(new Bundle());
        Meta bfdTransactionMeta = new Meta();
        when(mockBfdClient.requestPatientFromServer(SYNTHETIC_BENE_ID, null, null).getMeta()).thenReturn(bfdTransactionMeta);

        //Mock create job
        when(mockQueue.createJob(any(), any(), any(), any(), any(), any(), any(), any(), any(), anyBoolean())).thenReturn(UUID.randomUUID());

        //Mock fetching request Url
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost:3002/v1/Group/1234567890/$export"));

        //Past date with Z offset
        String since = "2020-05-26T16:43:01.780Z";
        Response response = resource.export(organizationPrincipal, groupId, null, FHIRMediaTypes.NDJSON, since, "respond-async", request);
        assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus(), "Expected ACCEPTED response code");

        //Past date with +10:00 offset
        since = "2020-05-26T16:43:01.780+10:00";
        response = resource.export(organizationPrincipal, groupId, null, FHIRMediaTypes.NDJSON, since, "respond-async", request);
        assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus(), "Expected ACCEPTED response code");

        //A few seconds ago using -04:00 offset
        since = OffsetDateTime.now(ZoneId.of("America/Puerto_Rico")).minusSeconds(5).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        response = resource.export(organizationPrincipal, groupId, null, FHIRMediaTypes.NDJSON, since, "respond-async", request);
        assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus(), "Expected ACCEPTED response code");

        verify(request, times(3)).getHeader(HttpHeaders.X_FORWARDED_FOR);
        verify(request, times(3)).getRemoteAddr();
    }

    @Test
    public void testExportWithInvalidTimes() {
        UUID orgId = UUID.randomUUID();
        Organization organization = new Organization();
        organization.setId(orgId.toString());
        Identifier identifier = new Identifier();
        identifier.setSystem(DPCIdentifierSystem.NPPES.getSystem()).setValue(NPIUtil.generateNPI());
        organization.setIdentifier(List.of(identifier));
        OrganizationPrincipal organizationPrincipal = new OrganizationPrincipal(organization);

        String groupId = "123456789";

        //Mock Group
        Group group = new Group();
        group.setId(groupId);
        group.addMember();
        group.addCharacteristic().getCode().addCoding().setCode("attributed-to");
        CodeableConcept codeableConcept = new CodeableConcept();
        codeableConcept.addCoding().setSystem(DPCIdentifierSystem.NPPES.getSystem()).setCode(NPIUtil.generateNPI());
        group.getCharacteristicFirstRep().setValue(codeableConcept);

        IReadExecutable<Group> readExec = mock(IReadExecutable.class);
        when(attributionClient.read().resource(Group.class).withId(new IdType("Group", groupId)).encodedJson()).thenReturn(readExec);
        when(readExec.execute()).thenReturn(group);

        IReadExecutable<Organization> readExec2 = mock(IReadExecutable.class);
        when(attributionClient.read().resource(Organization.class).withId(new IdType("Organization", orgId.toString())).encodedJson()).thenReturn(readExec2);
        when(readExec2.execute()).thenReturn(organization);

        //Mock fetching request Url
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost:3002/v1/Group/1234567890/$export"));

        //Mock get bundle
        IOperationUntypedWithInput<Bundle> bundleOperation = mock(IOperationUntypedWithInput.class);
        when(attributionClient
                .operation()
                .onInstance(new IdType(groupId))
                .named("patients")
                .withParameters(any(Parameters.class))
                .returnResourceType(Bundle.class)
                .useHttpGet()
                .encodedJson()).thenReturn(bundleOperation);

        when(bundleOperation.execute()).thenReturn(new Bundle());
        Meta bfdTransactionMeta = new Meta();
        when(mockBfdClient.requestPatientFromServer(SYNTHETIC_BENE_ID, null, null).getMeta()).thenReturn(bfdTransactionMeta);

        //Test a few seconds into the future
        WebApplicationException exception = Assertions.assertThrows(BadRequestException.class, () -> {
            String since = OffsetDateTime.now(ZoneId.of("America/Puerto_Rico")).plusSeconds(10).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            resource.export(organizationPrincipal, groupId, null, FHIRMediaTypes.NDJSON, since, "respond-async", request);
        });

        assertEquals("'_since' query parameter cannot be a future date", exception.getMessage());

        //Test a few days into the future
        exception = Assertions.assertThrows(BadRequestException.class, () -> {
            final String since = OffsetDateTime.now().plusDays(2).toString();
            resource.export(organizationPrincipal, groupId, null, FHIRMediaTypes.NDJSON, since, "respond-async", request);
        });

        assertEquals("'_since' query parameter cannot be a future date", exception.getMessage());

        //Test bad format
        exception = Assertions.assertThrows(WebApplicationException.class, () -> {
            final String since = "2020-05-2X616:43:01.780+10:00";
            resource.export(organizationPrincipal, groupId, null, FHIRMediaTypes.NDJSON, since, "respond-async", request);
        });

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), exception.getResponse().getStatus());

        verifyNoInteractions(request);
    }

    @Test
    public void testOutputFormatSetting() {
        UUID orgId = UUID.randomUUID();
        Organization organization = new Organization();
        organization.setId(orgId.toString());
        Identifier identifier = new Identifier();
        identifier.setSystem(DPCIdentifierSystem.NPPES.getSystem()).setValue(NPIUtil.generateNPI());
        organization.setIdentifier(List.of(identifier));
        OrganizationPrincipal organizationPrincipal = new OrganizationPrincipal(organization);

        IReadExecutable<Group> readExec = mock(IReadExecutable.class);
        Group fakeGroup = new Group();
        fakeGroup.getMember().add(new Group.GroupMemberComponent());
        fakeGroup.addCharacteristic().getCode().addCoding().setCode("attributed-to");
        CodeableConcept codeableConcept = new CodeableConcept();
        codeableConcept.addCoding().setSystem(DPCIdentifierSystem.NPPES.getSystem()).setCode(NPIUtil.generateNPI());
        fakeGroup.getCharacteristicFirstRep().setValue(codeableConcept);
        when(attributionClient
                .read()
                .resource(Group.class)
                .withId(any(IdType.class))
                .encodedJson())
                .thenReturn(readExec);

        when(readExec.execute())
                .thenReturn(fakeGroup);

        IReadExecutable<Organization> readExec2 = mock(IReadExecutable.class);
        when(attributionClient.read().resource(Organization.class).withId(new IdType("Organization", orgId.toString())).encodedJson()).thenReturn(readExec2);
        when(readExec2.execute()).thenReturn(organization);

        IOperationUntypedWithInput<Bundle> operationInput = mock(IOperationUntypedWithInput.class);
        Patient fakePatient = new Patient();
        fakePatient.getIdentifier().add(new Identifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue("2S51C00AA00"));
        Bundle fakeBundle = new Bundle();
        fakeBundle.getEntry().add(new Bundle.BundleEntryComponent().setResource(fakePatient));
        when(attributionClient
                .operation()
                .onInstance(any(IdType.class))
                .named("patients")
                .withParameters(any(Parameters.class))
                .returnResourceType(Bundle.class)
                .useHttpGet()
                .encodedJson())
                .thenReturn(operationInput);
        when(operationInput.execute())
                .thenReturn(fakeBundle);

        when(mockBfdClient.requestPatientFromServer(anyString(), any(), any()))
                .thenReturn(new Bundle());

        //Mock create job
        when(mockQueue.createJob(any(), any(), any(), any(), any(), any(), any(), any(), any(), anyBoolean())).thenReturn(UUID.randomUUID());

        //Mock fetching request Url
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost:3002/v1/Group/1234567890/$export"));

        Assertions.assertDoesNotThrow(() -> {
            resource.export(organizationPrincipal, "roster-id", "Coverage", FHIRMediaTypes.APPLICATION_NDJSON, "2017-01-01T00:00:00Z", "respond-async", request);
        });

        Assertions.assertDoesNotThrow(() -> {
            resource.export(organizationPrincipal, "roster-id", "Coverage", FHIRMediaTypes.FHIR_NDJSON, "2017-01-01T00:00:00Z", "respond-async", request);
        });

        Assertions.assertDoesNotThrow(() -> {
            resource.export(organizationPrincipal, "roster-id", "Coverage", FHIRMediaTypes.NDJSON, "2017-01-01T00:00:00Z", "respond-async", request);
        });

        Assertions.assertThrows(BadRequestException.class, () -> resource.export(organizationPrincipal, "roster-id", "Coverage", FHIR_JSON, "2017-01-01T00:00:00Z", "respond-async", request));

        Assertions.assertThrows(BadRequestException.class, () -> resource.export(organizationPrincipal, "roster-id", "Coverage", null, "2017-01-01T00:00:00Z", "respond-async", request));

        Assertions.assertThrows(BadRequestException.class, () -> resource.export(organizationPrincipal, "roster-id", "Coverage", "", "2017-01-01T00:00:00Z", "respond-async", request));

        //3 non bad requests
        verify(request, times(3)).getHeader(HttpHeaders.X_FORWARDED_FOR);
        verify(request, times(3)).getRemoteAddr();
    }

}
