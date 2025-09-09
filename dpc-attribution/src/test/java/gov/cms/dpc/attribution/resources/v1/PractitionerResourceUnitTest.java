package gov.cms.dpc.attribution.resources.v1;

import gov.cms.dpc.attribution.AttributionTestHelpers;
import gov.cms.dpc.attribution.DPCAttributionConfiguration;
import gov.cms.dpc.attribution.jdbi.ProviderDAO;
import gov.cms.dpc.common.entities.OrganizationEntity;
import gov.cms.dpc.common.entities.ProviderEntity;
import gov.cms.dpc.fhir.converters.FHIREntityConverter;
import jakarta.ws.rs.core.Response;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static gov.cms.dpc.attribution.AttributionTestHelpers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class PractitionerResourceUnitTest {

    private PractitionerResource practitionerResource;

    @Mock
    ProviderDAO providerDAO;

    @Mock
    DPCAttributionConfiguration attributionConfiguration;

    @Mock
    private final FHIREntityConverter converter = FHIREntityConverter.initialize();

    private final int maxProviders = 5;


    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(attributionConfiguration.getProviderLimit()).thenReturn(maxProviders);
        when(attributionConfiguration.getDbBatchSize()).thenReturn(100);
        practitionerResource = new PractitionerResource(converter, providerDAO, attributionConfiguration);
    }

    @Test
    void testPractitionerLimitReached() {
        OrganizationEntity org = createOrganizationEntity();
        List<ProviderEntity> existingProviders = new ArrayList<>();
        for (int i=0; i<maxProviders; i++) {
            ProviderEntity pe = AttributionTestHelpers.createProviderEntity(org);
            existingProviders.add(pe);
        }
        final Practitioner practitioner1 = AttributionTestHelpers.createPractitionerResource("1111111112");

        // mock fhir converter to return test data
        when(converter.fromFHIR(ProviderEntity.class, practitioner1)).thenReturn(AttributionTestHelpers.createProviderEntity(org));
        // mock DAO calls so we already have 5 results
        when(providerDAO.getProvidersCount(null, null, org.getId())).thenReturn((long) PROVIDER_LIMIT);
        when(providerDAO.getProviders(null, "1111111112", org.getId()))
                .thenReturn(existingProviders);


        Response response = this.practitionerResource.submitProvider(practitioner1);
        OperationOutcome opOutcome = (OperationOutcome) response.getEntity();

        assertEquals(422, response.getStatus());
        assertEquals("Provider limit reached", opOutcome.getIssueFirstRep().getDiagnostics());
    }
}
