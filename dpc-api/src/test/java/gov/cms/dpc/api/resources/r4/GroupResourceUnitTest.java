package gov.cms.dpc.api.resources.r4;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IReadExecutable;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.google.common.net.HttpHeaders;
import gov.cms.dpc.api.DPCAPIConfiguration;
import gov.cms.dpc.api.V3APITestHelpers;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.attribution.client.AttributionClient;
import gov.cms.dpc.common.utils.NPIUtil;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.FHIRMediaTypes;
import gov.cms.dpc.oneecosystem.client.OneEcosystemClient;
import gov.cms.dpc.oneecosystem.models.OneEcosystemExportRequest;
import gov.cms.dpc.oneecosystem.models.OneEcosystemExportResponse;
import gov.cms.dpc.queue.service.DataService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

@SuppressWarnings("unchecked")
public class GroupResourceUnitTest {

    private static final String BASE_URL = "http://localhost:3002/v3";

    @Mock
    AttributionClient attributionClient;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    IGenericClient attributionSTU3Client;

    @Mock
    OneEcosystemClient oneEcosystemClient;

    @Mock
    DataService mockDataService;

    @Mock
    HttpServletRequest request;

    DPCAPIConfiguration config = new DPCAPIConfiguration();

    GroupResource resource;

    @BeforeEach
    void setUp() {
        openMocks(this);
        resource = new GroupResource(attributionClient, attributionSTU3Client, BASE_URL, oneEcosystemClient, mockDataService, config);
    }

    private void stubOrgAndProviderNpi(OrganizationPrincipal organizationPrincipal, Group group) {
        // update when Organization is updated to r4
        org.hl7.fhir.dstu3.model.Organization organization = organizationPrincipal.getOrganization();
        UUID orgId = gov.cms.dpc.fhir.stu3.FHIRExtractors.getEntityUUID(organization.getId());

        org.hl7.fhir.dstu3.model.Identifier orgNpiIdentifier = new org.hl7.fhir.dstu3.model.Identifier();
        orgNpiIdentifier.setSystem(DPCIdentifierSystem.NPPES.getSystem()).setValue(NPIUtil.generateNPI());
        organization.setIdentifier(List.of(orgNpiIdentifier));

        group.addCharacteristic().getCode().addCoding().setCode("attributed-to");
        CodeableConcept providerNpiConcept = new CodeableConcept();
        providerNpiConcept.addCoding().setSystem(DPCIdentifierSystem.NPPES.getSystem()).setCode(NPIUtil.generateNPI());
        group.getCharacteristicFirstRep().setValue(providerNpiConcept);

        IReadExecutable<org.hl7.fhir.dstu3.model.Organization> orgReadExec = mock(IReadExecutable.class);
        when(attributionSTU3Client.read().resource(org.hl7.fhir.dstu3.model.Organization.class).withId(orgId.toString()).encodedJson()).thenReturn(orgReadExec);
        when(orgReadExec.execute()).thenReturn(organization);
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

        UUID createdGroupId = UUID.randomUUID();
        group.setId("https://attribution.internal:9902/v1/Group/" + createdGroupId);

        MethodOutcome outcome = new MethodOutcome();
        outcome.setResource(group);
        outcome.setCreated(true);
        when(attributionClient.createRoster(group)).thenReturn(outcome);

        org.hl7.fhir.dstu3.model.Identifier identifier = new org.hl7.fhir.dstu3.model.Identifier();
        identifier.setSystem(DPCIdentifierSystem.NPPES.getSystem());
        identifier.setValue(practitionerNPI);
        org.hl7.fhir.dstu3.model.Practitioner practitioner = new org.hl7.fhir.dstu3.model.Practitioner();
        practitioner.setIdentifier(List.of(identifier));
        IReadExecutable<org.hl7.fhir.dstu3.model.Practitioner> readExec = mock(IReadExecutable.class);
        when(attributionSTU3Client.read().resource(org.hl7.fhir.dstu3.model.Practitioner.class).withId(practitionerId.toString()).encodedJson()).thenReturn(readExec);
        when(readExec.execute()).thenReturn(practitioner);

        Response response = resource.createRoster(organizationPrincipal, provenance, group);
        Group result = (Group) response.getEntity();

        assertEquals("Organization ID", result.getMeta().getTag(DPCIdentifierSystem.DPC.getSystem(), orgId.toString()).getDisplay());
        assertEquals(group, result);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertEquals(BASE_URL + "/Group/" + createdGroupId, response.getHeaderString("Location"));
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

        Group groupWithBadTag = new Group();
        groupWithBadTag.getMeta().addTag(DPCIdentifierSystem.DPC.getSystem(), UUID.randomUUID().toString(), "Organization ID");
        groupWithBadTag.addCharacteristic().getCode().addCoding().setCode("attributed-to");
        CodeableConcept codeableConcept = new CodeableConcept();
        codeableConcept.addCoding().setSystem(DPCIdentifierSystem.NPPES.getSystem()).setCode(practitionerNPI);
        groupWithBadTag.getCharacteristicFirstRep().setValue(codeableConcept);

        org.hl7.fhir.dstu3.model.Identifier identifier = new org.hl7.fhir.dstu3.model.Identifier();
        identifier.setSystem(DPCIdentifierSystem.NPPES.getSystem());
        identifier.setValue(practitionerNPI);
        org.hl7.fhir.dstu3.model.Practitioner practitioner = new org.hl7.fhir.dstu3.model.Practitioner();
        practitioner.setIdentifier(List.of(identifier));
        IReadExecutable<org.hl7.fhir.dstu3.model.Practitioner> readExec = mock(IReadExecutable.class);
        when(attributionSTU3Client.read().resource(org.hl7.fhir.dstu3.model.Practitioner.class).withId(practitionerId.toString()).encodedJson()).thenReturn(readExec);
        when(readExec.execute()).thenReturn(practitioner);

        MethodOutcome outcome = new MethodOutcome();
        outcome.setResource(new Group());
        when(attributionClient.createRoster(any(Group.class))).thenReturn(outcome);

        resource.createRoster(organizationPrincipal, provenance, groupWithBadTag);

        ArgumentCaptor<Group> groupArg = ArgumentCaptor.forClass(Group.class);
        verify(attributionClient, times(1)).createRoster(groupArg.capture());

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

        org.hl7.fhir.dstu3.model.Identifier identifier = new org.hl7.fhir.dstu3.model.Identifier();
        identifier.setSystem(DPCIdentifierSystem.NPPES.getSystem());
        identifier.setValue(NPIUtil.generateNPI());
        org.hl7.fhir.dstu3.model.Practitioner practitioner = new org.hl7.fhir.dstu3.model.Practitioner();
        practitioner.setIdentifier(List.of(identifier));
        IReadExecutable<org.hl7.fhir.dstu3.model.Practitioner> readExec = mock(IReadExecutable.class);
        when(attributionSTU3Client.read().resource(org.hl7.fhir.dstu3.model.Practitioner.class).withId(practitionerId.toString()).encodedJson()).thenReturn(readExec);
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

        IReadExecutable<org.hl7.fhir.dstu3.model.Practitioner> readExec = mock(IReadExecutable.class);
        when(attributionSTU3Client.read().resource(org.hl7.fhir.dstu3.model.Practitioner.class).withId(practitionerId.toString()).encodedJson()).thenReturn(readExec);
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

        stubOrgAndProviderNpi(organizationPrincipal, group);

        when(attributionClient.getRoster(UUID.fromString(groupId))).thenReturn(group);

        Bundle patients = new Bundle();
        patients.addEntry(new Bundle.BundleEntryComponent().setResource(V3APITestHelpers.createPatientWithMbi("9S79A00AA00")));
        when(attributionClient.getAttributedPatients(UUID.fromString(groupId), true)).thenReturn(patients);

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

        stubOrgAndProviderNpi(organizationPrincipal, group);

        when(attributionClient.getRoster(UUID.fromString(groupId))).thenReturn(group);

        when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost:3002/v3/Group/" + groupId + "/$export"));

        Bundle patients = new Bundle();
        patients.addEntry(new Bundle.BundleEntryComponent().setResource(V3APITestHelpers.createPatientWithMbi("9S79A00AA00")));
        when(attributionClient.getAttributedPatients(UUID.fromString(groupId), true)).thenReturn(patients);

        final String timezoneFutureSince = OffsetDateTime.now(ZoneId.of("America/Puerto_Rico")).plusSeconds(10).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            resource.export(organizationPrincipal, groupId, null, FHIRMediaTypes.NDJSON, timezoneFutureSince, null, "respond-async", request);
        });
        assertEquals("'_since' query parameter cannot be a future date", exception.getMessage());

        final String futureSince = OffsetDateTime.now().plusDays(2).toString();
        exception = assertThrows(BadRequestException.class, () -> {
            resource.export(organizationPrincipal, groupId, null, FHIRMediaTypes.NDJSON, futureSince, null, "respond-async", request);
        });
        assertEquals("'_since' query parameter cannot be a future date", exception.getMessage());

        final String badFormatSince = "2020-05-2X616:43:01.780+10:00";
        WebApplicationException badFormat = assertThrows(WebApplicationException.class, () -> {
            resource.export(organizationPrincipal, groupId, null, FHIRMediaTypes.NDJSON, badFormatSince, null, "respond-async", request);
        });
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), badFormat.getResponse().getStatus());

        verifyNoInteractions(request);
    }

    @ParameterizedTest(name = "testExportWithInvalidPreferHeader[prefer={0}]")
    @NullSource
    @ValueSource(strings = {"", "badPrefer"})
    void testExportWithInvalidPreferHeader(String prefer) {
        Organization organization = new Organization();
        OrganizationPrincipal organizationPrincipal = new OrganizationPrincipal(organization);

        assertThrows(BadRequestException.class, () ->
                resource.export(organizationPrincipal, "fakeId", null, FHIRMediaTypes.NDJSON, "fakeSince", null, prefer, request));
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

        stubOrgAndProviderNpi(organizationPrincipal, fakeGroup);

        when(attributionClient.getRoster(any(UUID.class))).thenReturn(fakeGroup);

        Patient fakePatient = V3APITestHelpers.createPatientWithMbi("2S51C00AA00");
        Bundle fakeBundle = new Bundle();
        fakeBundle.getEntry().add(new Bundle.BundleEntryComponent().setResource(fakePatient));
        when(attributionClient.getAttributedPatients(any(UUID.class), eq(true))).thenReturn(fakeBundle);

        when(oneEcosystemClient.export(any())).thenReturn(
                new OneEcosystemExportResponse(202, "https://localhost:3002/api/v3/jobs/" + UUID.randomUUID()));

        when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost:3002/v3/Group/" + groupId + "/$export"));

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

        when(attributionClient.getRoster(UUID.fromString(groupId))).thenReturn(group);
        when(attributionClient.getAttributedPatients(UUID.fromString(groupId), true)).thenReturn(new Bundle());

        String since = "2020-05-26T16:43:01.780Z";
        assertThrows(WebApplicationException.class, () ->
                resource.export(organizationPrincipal, groupId, null, FHIRMediaTypes.NDJSON, since, null, "respond-async", request));
    }
}
