package gov.cms.dpc.api.resources.r4;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationOptions;
import ca.uhn.fhir.validation.ValidationResult;
import gov.cms.dpc.api.auth.r4.OrganizationPrincipal;
import gov.cms.dpc.api.r4.APITestHelpers;
import gov.cms.dpc.attribution.client.AttributionClient;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Practitioner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PractitionerResourceUnitTest {

    private AttributionClient attributionClient;
    private FhirValidator fhirValidator;
    private PractitionerResource practitionerResource;

    @BeforeEach
    void setUp() {
        attributionClient = mock(AttributionClient.class);
        fhirValidator = mock(FhirValidator.class);
        practitionerResource = new PractitionerResource(attributionClient, fhirValidator);
    }

    @Test
    void testPractitionerSearch() {
        OrganizationPrincipal principal = APITestHelpers.makeOrganizationPrincipal();
        String providerNPI = "1232131239";
        Bundle bundle = new Bundle();
        when(attributionClient.searchPractitioners(principal.getID(), providerNPI)).thenReturn(bundle);

        assertSame(bundle, practitionerResource.practitionerSearch(principal, providerNPI));
    }

    @Test
    void testGetProvider() {
        UUID providerId = UUID.randomUUID();
        Practitioner practitioner = new Practitioner();
        practitioner.setId(providerId.toString());
        when(attributionClient.getPractitioner(providerId)).thenReturn(practitioner);

        assertSame(practitioner, practitionerResource.getProvider(providerId));
    }

    @Test
    void testSubmitProvider() {
        OrganizationPrincipal principal = APITestHelpers.makeOrganizationPrincipal();
        Practitioner practitioner = new Practitioner();
        practitioner.getMeta().addTag(DPCIdentifierSystem.DPC.getSystem(), UUID.randomUUID().toString(), "old_tag");
        MethodOutcome outcome = new MethodOutcome();
        outcome.setResource(practitioner);
        when(attributionClient.createPractitioner(practitioner)).thenReturn(outcome);

        Response response = practitionerResource.submitProvider(principal, practitioner);

        assertSame(practitioner, response.getEntity());
        assertEquals(1, practitioner.getMeta().getTag().size());
        assertEquals("Organization ID", practitioner.getMeta()
                .getTag(DPCIdentifierSystem.DPC.getSystem(), principal.getID().toString()).getDisplay());
        verify(attributionClient).createPractitioner(practitioner);
    }

    @Test
    void testBulkSubmitProviders() {
        OrganizationPrincipal principal = APITestHelpers.makeOrganizationPrincipal();
        Practitioner practitioner = new Practitioner();
        practitioner.getMeta().addTag(DPCIdentifierSystem.DPC.getSystem(), UUID.randomUUID().toString(), "old_tag");
        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(practitioner);
        Parameters params = new Parameters();
        params.addParameter().setResource(bundle);

        ValidationResult validationResult = mock(ValidationResult.class);
        when(validationResult.isSuccessful()).thenReturn(true);
        when(fhirValidator.validateWithResult(eq(practitioner), any(ValidationOptions.class)))
                .thenReturn(validationResult);
        when(attributionClient.submitPractitioners(bundle)).thenReturn(bundle);

        assertSame(bundle, practitionerResource.bulkSubmitProviders(principal, params));
        assertEquals(1, practitioner.getMeta().getTag().size());
        assertNotNull(practitioner.getMeta().getTag(DPCIdentifierSystem.DPC.getSystem(), principal.getID().toString()));
        verify(attributionClient).submitPractitioners(bundle);
    }

    @Test
    void testBulkSubmitRejectsInvalidPractitioner() {
        OrganizationPrincipal principal = APITestHelpers.makeOrganizationPrincipal();
        Practitioner practitioner = new Practitioner();
        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(practitioner);
        Parameters params = new Parameters();
        params.addParameter().setResource(bundle);

        ValidationResult result = mock(ValidationResult.class);
        when(result.getMessages()).thenReturn(java.util.List.of());
        when(fhirValidator.validateWithResult(eq(practitioner), any(ValidationOptions.class))).thenReturn(result);

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> practitionerResource.bulkSubmitProviders(principal, params));

        assertEquals(422, exception.getResponse().getStatus());
        verifyNoInteractions(attributionClient);
    }

    @Test
    void testDeleteProvider() {
        UUID providerId = UUID.randomUUID();

        Response response = practitionerResource.deleteProvider(providerId);

        assertEquals(200, response.getStatus());
        verify(attributionClient).deletePractitioner(providerId);
    }

    @Test
    void testUpdateProvider() {
        UUID providerId = UUID.randomUUID();
        Practitioner practitioner = new Practitioner();
        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> practitionerResource.updateProvider(providerId, practitioner));
        assertEquals(501, exception.getResponse().getStatus());
        verifyNoInteractions(attributionClient);
    }

    @Test
    void testValidateProvider() {
        OrganizationPrincipal principal = APITestHelpers.makeOrganizationPrincipal();
        Parameters params = new Parameters();
        Bundle bundle = new Bundle();
        params.addParameter().setResource(bundle);
        ValidationResult validationResult = mock(ValidationResult.class);
        OperationOutcome outcome = new OperationOutcome();
        when(fhirValidator.validateWithResult(eq(bundle), any(ValidationOptions.class))).thenReturn(validationResult);
        when(validationResult.toOperationOutcome()).thenReturn(outcome);

        assertSame(outcome, practitionerResource.validateProvider(principal, params));
    }
}
