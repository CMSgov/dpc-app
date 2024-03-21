package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.*;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationOptions;
import ca.uhn.fhir.validation.ValidationResult;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.common.utils.NPIUtil;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.core.Response;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class PractitionerResourceUnitTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    IGenericClient attributionClient;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    FhirValidator fhirValidator;

    PractitionerResource practitionerResource;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        practitionerResource = new PractitionerResource(attributionClient, fhirValidator);
    }

    @Test
    public void testPractitionerSearch() {
        UUID orgId = UUID.randomUUID();
        Organization organization = new Organization();
        organization.setId(orgId.toString());
        OrganizationPrincipal organizationPrincipal = new OrganizationPrincipal(organization);
        String providerNPI = NPIUtil.generateNPI();
        Map<String, List<String>> searchParams = new HashMap<>();
        searchParams.put("organization", Collections.singletonList(organizationPrincipal.getOrganization().getIdElement().getIdPart()));
        searchParams.put("identifier", Collections.singletonList(providerNPI));
        Bundle bundle = new Bundle();

        @SuppressWarnings("unchecked")
        IQuery<IBaseBundle> queryExec = Mockito.mock(IQuery.class, Answers.RETURNS_DEEP_STUBS);
        @SuppressWarnings("unchecked")
        IQuery<Bundle> mockQuery = Mockito.mock(IQuery.class);
        Mockito.when(attributionClient.search().forResource(Practitioner.class).encodedJson()).thenReturn(queryExec);
        Mockito.when(queryExec.returnBundle(Bundle.class)).thenReturn(mockQuery);
        Mockito.when(mockQuery.execute()).thenReturn(bundle);
        Mockito.when(mockQuery.whereMap(searchParams)).thenReturn(mockQuery);

        Bundle actualResponse = practitionerResource.practitionerSearch(organizationPrincipal, providerNPI);

        assertEquals(bundle, actualResponse);
    }

    @Test
    public void testGetProvider() {
        UUID providerId = UUID.randomUUID();
        Practitioner practitioner = new Practitioner();
        practitioner.setId(providerId.toString());

        @SuppressWarnings("unchecked")
        IReadExecutable<Practitioner> readExec = Mockito.mock(IReadExecutable.class);
        Mockito.when(attributionClient.read().resource(Practitioner.class).withId(providerId.toString()).encodedJson()).thenReturn(readExec);
        Mockito.when(readExec.execute()).thenReturn(practitioner);

        Practitioner actualResponse = practitionerResource.getProvider(providerId);

        assertEquals(practitioner, actualResponse);
    }

    @Test
    public void testSubmitProvider() {
        UUID orgId = UUID.randomUUID();
        Organization organization = new Organization();
        organization.setId(orgId.toString());
        OrganizationPrincipal organizationPrincipal = new OrganizationPrincipal(organization);
        Practitioner practitioner = new Practitioner();

        ICreateTyped createExec = Mockito.mock(ICreateTyped.class);
        Mockito.when(attributionClient.create().resource(practitioner).encodedJson()).thenReturn(createExec);

        MethodOutcome outcome = new MethodOutcome();
        outcome.setResource(practitioner);
        Mockito.when(createExec.execute()).thenReturn(outcome);

        Response response = practitionerResource.submitProvider(organizationPrincipal, practitioner);
        Practitioner result = (Practitioner) response.getEntity();

        assertEquals(practitioner, result);
        assertEquals("Organization ID", practitioner.getMeta().getTag(DPCIdentifierSystem.DPC.getSystem(), orgId.toString()).getDisplay());
    }

    @Test
    public void testBulkSubmitProviders() {
        UUID orgId = UUID.randomUUID();
        Organization organization = new Organization();
        organization.setId(orgId.toString());
        OrganizationPrincipal organizationPrincipal = new OrganizationPrincipal(organization);
        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(new Practitioner());
        Parameters params = new Parameters();
        params.addParameter().setResource(bundle);

        @SuppressWarnings("unchecked")
        IOperationUntypedWithInput<Bundle> practitionerBundle = Mockito.mock(IOperationUntypedWithInput.class);
        Mockito.when(attributionClient
            .operation()
            .onType(Practitioner.class)
            .named("submit")
            .withParameters(Mockito.any())
            .returnResourceType(Bundle.class)
            .encodedJson()
        ).thenReturn(practitionerBundle);
        Mockito.when(practitionerBundle.execute()).thenReturn(bundle);

        Bundle actualResponse = practitionerResource.bulkSubmitProviders(organizationPrincipal, params);

        assertEquals(bundle, actualResponse);
    }

    @Test
    public void testDeleteProvider() {
        UUID providerId = UUID.randomUUID();

        IDeleteTyped delResp = Mockito.mock(IDeleteTyped.class);
        Mockito.when(attributionClient.delete().resourceById(new IdType("Practitioner", providerId.toString())).encodedJson()).thenReturn(delResp);

        Response actualResponse = practitionerResource.deleteProvider(providerId);

        assertEquals(200, actualResponse.getStatus());
    }

    @Test
    public void testUpdateProvider() {
        // Not yet implemented
    }

    @Test
    public void testValidateProvider() {
        UUID orgId = UUID.randomUUID();
        Organization organization = new Organization();
        organization.setId(orgId.toString());
        OrganizationPrincipal organizationPrincipal = new OrganizationPrincipal(organization);
        Parameters params = new Parameters();
        params.addParameter().setResource(new Bundle());

        ValidationResult validationResult = Mockito.mock(ValidationResult.class);
        Resource resource = params.getParameterFirstRep().getResource();
        ValidationOptions valOps = new ValidationOptions();
        String practitioner_profile_uri = "https://dpc.cms.gov/api/v1/StructureDefinition/dpc-profile-practitioner";
        valOps.addProfile(practitioner_profile_uri);
        Mockito.when(
            fhirValidator.validateWithResult(resource, valOps)
        ).thenReturn(validationResult);

        IBaseOperationOutcome actualResponse = practitionerResource.validateProvider(organizationPrincipal, params);

        assertNotNull(actualResponse);
    }
}
