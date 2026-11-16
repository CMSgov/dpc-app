package gov.cms.dpc.api.resources.r4;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICreate;
import ca.uhn.fhir.rest.gclient.ICreateTyped;
import ca.uhn.fhir.rest.gclient.IOperationUntypedWithInput;
import ca.uhn.fhir.rest.gclient.IReadExecutable;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.google.common.net.HttpHeaders;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.common.utils.NPIUtil;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.FHIRMediaTypes;
import gov.cms.dpc.oneecosystem.client.OneEcosystemClient;
import gov.cms.dpc.oneecosystem.models.OneEcosystemExportRequest;
import gov.cms.dpc.oneecosystem.models.OneEcosystemExportResponse;
import gov.cms.dpc.queue.IJobQueue;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import static gov.cms.dpc.fhir.FHIRMediaTypes.FHIR_JSON;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

@SuppressWarnings("unchecked")
public class GroupResourceUnitTest {

    private static final String BASE_URL = "http://localhost:3002/v3";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    IGenericClient attributionClient;

    @Mock
    OneEcosystemClient oneEcosystemClient;

    @Mock
    IJobQueue mockQueue;

    @Mock
    HttpServletRequest request;

    GroupResource resource;

    @BeforeEach
    void setUp() {
        openMocks(this);
        resource = new GroupResource(attributionClient, BASE_URL, oneEcosystemClient, mockQueue);
    }

    @Test
    void testCreateRoster() {
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
    void testCreateRosterUsesCorrectTag() {
        UUID orgId = UUID.randomUUID();
        Organization organization = new Organization();
        organization.setId(orgId.toString());
        OrganizationPrincipal organizationPrincipal = new OrganizationPrincipal(organization);

        String practitionerNPI = NPIUtil.generateNPI();
        UUID practitionerId = UUID.randomUUID();

        Provenance provenance = new Provenance();
        Provenance.ProvenanceAgentComponent provenanceAgent = new Provenance.ProvenanceAgentComponent();
        provenanceAgent.addRole().addCoding().setCode("AGNT");
        provenanceAgent.setWho(new Reference("Organization/" + UUID.randomUUID()));
        provenanceAgent.setOnBehalfOf(new Reference("Practitioner/" + practitionerId));
        provenance.addAgent(provenanceAgent);

        // Pre-populate a tag for a different org, to verify createRoster overwrites it
        // rather than appending.
        Group groupWithBadTag = new Group();
        groupWithBadTag.getMeta().addTag(DPCIdentifierSystem.DPC.getSystem(), UUID.randomUUID().toString(), "Organization ID");
        groupWithBadTag.addCharacteristic().getCode().addCoding().setCode("attributed-to");
        CodeableConcept codeableConcept = new CodeableConcept();
        codeableConcept.addCoding().setSystem(DPCIdentifierSystem.NPPES.getSystem()).setCode(practitionerNPI);
        groupWithBadTag.getCharacteristicFirstRep().setValue(codeableConcept);

        Identifier identifier = new Identifier();
        identifier.setSystem(DPCIdentifierSystem.NPPES.getSystem());
        identifier.setValue(practitionerNPI);
        Practitioner practitioner = new Practitioner();
        practitioner.setIdentifier(List.of(identifier));
        IReadExecutable<Practitioner> readExec = mock(IReadExecutable.class);
        when(attributionClient.read().resource(Practitioner.class).withId(practitionerId.toString()).encodedJson()).thenReturn(readExec);
        when(readExec.execute()).thenReturn(practitioner);

        ICreate iCreate = mock(ICreate.class);
        doReturn(iCreate).when(attributionClient).create();

        ICreateTyped iCreateTyped = mock(ICreateTyped.class);
        doReturn(iCreateTyped).when(iCreate).resource(groupWithBadTag);
        doReturn(iCreateTyped).when(iCreateTyped).encodedJson();

        MethodOutcome outcome = new MethodOutcome();
        outcome.setResource(new Group());
        doReturn(outcome).when(iCreateTyped).execute();

        resource.createRoster(organizationPrincipal, provenance, groupWithBadTag);

        ArgumentCaptor<Group> groupArg = ArgumentCaptor.forClass(Group.class);
        verify(iCreate, times(1)).resource(groupArg.capture());

        Group capturedGroup = groupArg.getValue();
        assertEquals(organization.getId(), capturedGroup.getMeta().getTagFirstRep().getCode());
    }

    @Test
    void testCreateRosterNonMatchingNPI() {
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

        Identifier identifier = new Identifier();
        identifier.setSystem(DPCIdentifierSystem.NPPES.getSystem());
        identifier.setValue(NPIUtil.generateNPI());
        Practitioner practitioner = new Practitioner();
        practitioner.setIdentifier(List.of(identifier));
        IReadExecutable<Practitioner> readExec = mock(IReadExecutable.class);
        when(attributionClient.read().resource(Practitioner.class).withId(practitionerId.toString()).encodedJson()).thenReturn(readExec);
        when(readExec.execute()).thenReturn(practitioner);

        assertThrows(WebApplicationException.class, () -> resource.createRoster(organizationPrincipal, provenance, group));
    }

    @Test
    void testCreateRosterProviderNotFound() {
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

        IReadExecutable<Practitioner> readExec = mock(IReadExecutable.class);
        when(attributionClient.read().resource(Practitioner.class).withId(practitionerId.toString()).encodedJson()).thenReturn(readExec);
        when(readExec.execute()).thenThrow(new ResourceNotFoundException("practitioner not found"));

        assertThrows(WebApplicationException.class, () -> resource.createRoster(organizationPrincipal, provenance, group));
    }

    @Test
    void testExportWithValidSinceParam() {
        Organization organization = new Organization();
        organization.setId(UUID.randomUUID().toString());
        OrganizationPrincipal organizationPrincipal = new OrganizationPrincipal(organization);

        String groupId = UUID.randomUUID().toString();

        Group group = new Group();
        group.setId(groupId);
        group.addMember().setEntity(new Reference("Patient/" + UUID.randomUUID()));

        IReadExecutable<Group> readExec = mock(IReadExecutable.class);
        when(attributionClient.read().resource(Group.class).withId(new IdType("Group", groupId)).encodedJson()).thenReturn(readExec);
        when(readExec.execute()).thenReturn(group);

        IOperationUntypedWithInput<Bundle> bundleOperation = mock(IOperationUntypedWithInput.class);
        when(attributionClient
                .operation()
                .onInstance(new IdType(group.getId()))
                .named("patients")
                .withParameters(any(Parameters.class))
                .returnResourceType(Bundle.class)
                .useHttpGet()
                .encodedJson()).thenReturn(bundleOperation);

        Bundle patients = new Bundle();
        patients.addEntry(new Bundle.BundleEntryComponent()
                .setResource(new Patient()
                        .setIdentifier(List.of(new Identifier()
                                .setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue("9S79A00AA00")))));
        when(bundleOperation.execute()).thenReturn(patients);

        when(oneEcosystemClient.export(any())).thenReturn(
                new OneEcosystemExportResponse(202, "https://localhost:3002/api/v3/jobs/" + UUID.randomUUID()));

        when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost:3002/v3/Group/" + groupId + "/$export"));

        String since = "2020-05-26T16:43:01.780Z";
        Response response = resource.export(organizationPrincipal, groupId, null, FHIRMediaTypes.NDJSON, since, null, "respond-async", request);
        assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus(), "Expected ACCEPTED response code");

        since = "2020-05-26T16:43:01.780+10:00";
        response = resource.export(organizationPrincipal, groupId, null, FHIRMediaTypes.NDJSON, since, null, "respond-async", request);
        assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus(), "Expected ACCEPTED response code");

        since = OffsetDateTime.now(ZoneId.of("America/Puerto_Rico")).minusSeconds(5).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        response = resource.export(organizationPrincipal, groupId, null, FHIRMediaTypes.NDJSON, since, null, "respond-async", request);
        assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus(), "Expected ACCEPTED response code");

        verify(request, times(3)).getHeader(HttpHeaders.X_FORWARDED_FOR);
        verify(request, times(3)).getRemoteAddr();
    }

    @Test
    void testExportWithInvalidTimes() {
        Organization organization = new Organization();
        organization.setId(UUID.randomUUID().toString());
        OrganizationPrincipal organizationPrincipal = new OrganizationPrincipal(organization);

        String groupId = UUID.randomUUID().toString();

        Group group = new Group();
        group.setId(groupId);
        group.addMember().setEntity(new Reference("Patient/" + UUID.randomUUID()));

        IReadExecutable<Group> readExec = mock(IReadExecutable.class);
        when(attributionClient.read().resource(Group.class).withId(new IdType("Group", groupId)).encodedJson()).thenReturn(readExec);
        when(readExec.execute()).thenReturn(group);

        when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost:3002/v3/Group/" + groupId + "/$export"));

        IOperationUntypedWithInput<Bundle> bundleOperation = mock(IOperationUntypedWithInput.class);
        when(attributionClient
                .operation()
                .onInstance(new IdType(group.getId()))
                .named("patients")
                .withParameters(any(Parameters.class))
                .returnResourceType(Bundle.class)
                .useHttpGet()
                .encodedJson()).thenReturn(bundleOperation);

        Bundle patients = new Bundle();
        patients.addEntry(new Bundle.BundleEntryComponent()
                .setResource(new Patient()
                        .setIdentifier(List.of(new Identifier()
                                .setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue("9S79A00AA00")))));
        when(bundleOperation.execute()).thenReturn(patients);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            String since = OffsetDateTime.now(ZoneId.of("America/Puerto_Rico")).plusSeconds(10).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            resource.export(organizationPrincipal, groupId, null, FHIRMediaTypes.NDJSON, since, null, "respond-async", request);
        });
        assertEquals("'_since' query parameter cannot be a future date", exception.getMessage());

        exception = assertThrows(BadRequestException.class, () -> {
            final String since = OffsetDateTime.now().plusDays(2).toString();
            resource.export(organizationPrincipal, groupId, null, FHIRMediaTypes.NDJSON, since, null, "respond-async", request);
        });
        assertEquals("'_since' query parameter cannot be a future date", exception.getMessage());

        WebApplicationException badFormat = assertThrows(WebApplicationException.class, () -> {
            final String since = "2020-05-2X616:43:01.780+10:00";
            resource.export(organizationPrincipal, groupId, null, FHIRMediaTypes.NDJSON, since, null, "respond-async", request);
        });
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), badFormat.getResponse().getStatus());

        verifyNoInteractions(request);
    }

    @Test
    void testExportWithNullPreferHeader() {
        Organization organization = new Organization();
        OrganizationPrincipal organizationPrincipal = new OrganizationPrincipal(organization);

        assertThrows(BadRequestException.class, () ->
                resource.export(organizationPrincipal, "fakeId", null, FHIRMediaTypes.NDJSON, "fakeSince", null, null, request));
    }

    @Test
    void testExportWithEmptyPreferHeader() {
        Organization organization = new Organization();
        OrganizationPrincipal organizationPrincipal = new OrganizationPrincipal(organization);

        assertThrows(BadRequestException.class, () ->
                resource.export(organizationPrincipal, "fakeId", null, FHIRMediaTypes.NDJSON, "fakeSince", null, "", request));
    }

    @Test
    void testExportWithBadPreferHeader() {
        Organization organization = new Organization();
        OrganizationPrincipal organizationPrincipal = new OrganizationPrincipal(organization);

        assertThrows(BadRequestException.class, () ->
                resource.export(organizationPrincipal, "fakeId", null, FHIRMediaTypes.NDJSON, "fakeSince", null, "badPrefer", request));
    }

    @Test
    void testOutputFormatSetting() {
        Organization organization = new Organization();
        organization.setId(UUID.randomUUID().toString());
        OrganizationPrincipal organizationPrincipal = new OrganizationPrincipal(organization);

        String groupId = UUID.randomUUID().toString();
        Group fakeGroup = new Group();
        fakeGroup.setId(groupId);
        fakeGroup.addMember().setEntity(new Reference("Patient/" + UUID.randomUUID()));

        IReadExecutable<Group> readExec = mock(IReadExecutable.class);
        when(attributionClient
                .read()
                .resource(Group.class)
                .withId(any(IdType.class))
                .encodedJson())
                .thenReturn(readExec);
        when(readExec.execute()).thenReturn(fakeGroup);

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

        when(oneEcosystemClient.export(any())).thenReturn(
                new OneEcosystemExportResponse(202, "https://localhost:3002/api/v3/jobs/" + UUID.randomUUID()));

        when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost:3002/v3/Group/1234567890/$export"));

        assertDoesNotThrow(() ->
                resource.export(organizationPrincipal, groupId, "Coverage", FHIRMediaTypes.APPLICATION_NDJSON, "2017-01-01T00:00:00Z", null, "respond-async", request));

        assertDoesNotThrow(() ->
                resource.export(organizationPrincipal, groupId, "Coverage", FHIRMediaTypes.FHIR_NDJSON, "2017-01-01T00:00:00Z", null, "respond-async", request));

        assertDoesNotThrow(() ->
                resource.export(organizationPrincipal, groupId, "Coverage", FHIRMediaTypes.NDJSON, "2017-01-01T00:00:00Z", null, "respond-async", request));

        assertThrows(BadRequestException.class, () ->
                resource.export(organizationPrincipal, groupId, "Coverage", FHIR_JSON, "2017-01-01T00:00:00Z", null, "respond-async", request));

        assertThrows(BadRequestException.class, () ->
                resource.export(organizationPrincipal, groupId, "Coverage", null, "2017-01-01T00:00:00Z", null, "respond-async", request));

        assertThrows(BadRequestException.class, () ->
                resource.export(organizationPrincipal, groupId, "Coverage", "", "2017-01-01T00:00:00Z", null, "respond-async", request));

        // 3 non-bad requests
        verify(request, times(3)).getHeader(HttpHeaders.X_FORWARDED_FOR);
        verify(request, times(3)).getRemoteAddr();

        ArgumentCaptor<OneEcosystemExportRequest> exportRequestCaptor = ArgumentCaptor.forClass(OneEcosystemExportRequest.class);
        verify(oneEcosystemClient, times(3)).export(exportRequestCaptor.capture());
        exportRequestCaptor.getAllValues().forEach(req ->
                assertEquals(List.of("Coverage"), req.resourceTypes()));
    }

    @Test
    void testExportWithExpiredPatient() {
        Organization organization = new Organization();
        organization.setId(UUID.randomUUID().toString());
        OrganizationPrincipal organizationPrincipal = new OrganizationPrincipal(organization);

        String groupId = UUID.randomUUID().toString();

        Group group = new Group();
        group.setId(groupId);
        group.addMember().setEntity(new Reference("Patient/" + UUID.randomUUID()));

        IReadExecutable<Group> readExec = mock(IReadExecutable.class);
        when(attributionClient.read().resource(Group.class).withId(new IdType("Group", groupId)).encodedJson()).thenReturn(readExec);
        when(readExec.execute()).thenReturn(group);

        IOperationUntypedWithInput<Bundle> bundleOperation = mock(IOperationUntypedWithInput.class);
        when(attributionClient
                .operation()
                .onInstance(new IdType(group.getId()))
                .named("patients")
                .withParameters(any(Parameters.class))
                .returnResourceType(Bundle.class)
                .useHttpGet()
                .encodedJson()).thenReturn(bundleOperation);

        when(bundleOperation.execute()).thenReturn(new Bundle());

        String since = "2020-05-26T16:43:01.780Z";
        assertThrows(WebApplicationException.class, () ->
                resource.export(organizationPrincipal, groupId, null, FHIRMediaTypes.NDJSON, since, null, "respond-async", request));
    }
}
