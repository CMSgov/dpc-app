package gov.cms.dpc.api.auth.macaroonauth.r4;

import gov.cms.dpc.api.auth.DPCAuthCredentials;
import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import gov.cms.dpc.attribution.client.AttributionClient;
import gov.cms.dpc.attribution.client.search.PatientSearch;
import gov.cms.dpc.fhir.DPCResourceType;
import jakarta.ws.rs.NotFoundException;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MacaroonsAuthenticatorUnitTest {

    private final AttributionClient client = mock(AttributionClient.class);
    private final PathAuthorizer pathAuthorizer = mock(PathAuthorizer.class);
    private final Organization organization = new Organization();
    private final MacaroonsAuthenticator authenticator = new MacaroonsAuthenticator(client);
    private UUID organizationId;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        organization.setId("Organization/" + organizationId);
    }

    @Test
    void testAuthenticateWithoutPathAuthorizer() {
        DPCAuthCredentials credentials = new DPCAuthCredentials("macaroon", organization, null, null);

        assertSame(organization, authenticator.authenticate(credentials).orElseThrow().getOrganization());
        verifyNoInteractions(client);
    }

    @Test
    void testOrganizationPath() {
        when(pathAuthorizer.type()).thenReturn(DPCResourceType.Organization);
        DPCAuthCredentials own = new DPCAuthCredentials("macaroon", organization, pathAuthorizer, organizationId.toString());
        DPCAuthCredentials other = new DPCAuthCredentials("macaroon", organization, pathAuthorizer, UUID.randomUUID().toString());

        assertTrue(authenticator.authenticate(own).isPresent());
        assertTrue(authenticator.authenticate(other).isEmpty());
        verifyNoInteractions(client);
    }

    @Test
    void testPractitionerPath() {
        when(pathAuthorizer.type()).thenReturn(DPCResourceType.Practitioner);
        UUID practitionerId = UUID.randomUUID();
        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(new Practitioner());
        when(client.searchPractitioners(organizationId, practitionerId)).thenReturn(bundle);
        DPCAuthCredentials credentials = new DPCAuthCredentials("macaroon", organization, pathAuthorizer, practitionerId.toString());

        assertTrue(authenticator.authenticate(credentials).isPresent());
        verify(client).searchPractitioners(organizationId, practitionerId);
    }

    @Test
    void testPractitionerPathNotFound() {
        when(pathAuthorizer.type()).thenReturn(DPCResourceType.Practitioner);
        UUID practitionerId = UUID.randomUUID();
        when(client.searchPractitioners(organizationId, practitionerId)).thenReturn(new Bundle());
        DPCAuthCredentials credentials = new DPCAuthCredentials("macaroon", organization, pathAuthorizer, practitionerId.toString());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> authenticator.authenticate(credentials));
        assertEquals("Practitioner not found", exception.getMessage());
    }

    @Test
    void testPatientPath() {
        when(pathAuthorizer.type()).thenReturn(DPCResourceType.Patient);
        UUID patientId = UUID.randomUUID();
        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(new Patient());
        PatientSearch search = new PatientSearch(patientId, null, organizationId, null, null, null);
        when(client.searchPatients(search)).thenReturn(bundle);
        DPCAuthCredentials credentials = new DPCAuthCredentials("macaroon", organization, pathAuthorizer, patientId.toString());

        assertTrue(authenticator.authenticate(credentials).isPresent());
        verify(client).searchPatients(search);
    }

    @Test
    void testPatientPathNotFound() {
        when(pathAuthorizer.type()).thenReturn(DPCResourceType.Patient);
        UUID patientId = UUID.randomUUID();
        PatientSearch search = new PatientSearch(patientId, null, organizationId, null, null, null);
        when(client.searchPatients(search)).thenReturn(new Bundle());
        DPCAuthCredentials credentials = new DPCAuthCredentials("macaroon", organization, pathAuthorizer, patientId.toString());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> authenticator.authenticate(credentials));
        assertEquals("Patient not found", exception.getMessage());
    }

    @Test
    void testUnsupportedPathTypeFailsClosed() {
        when(pathAuthorizer.type()).thenReturn(DPCResourceType.Group);
        DPCAuthCredentials credentials = new DPCAuthCredentials("macaroon", organization, pathAuthorizer, UUID.randomUUID().toString());

        assertThrows(IllegalStateException.class, () -> authenticator.authenticate(credentials));
        verifyNoInteractions(client);
    }
}
