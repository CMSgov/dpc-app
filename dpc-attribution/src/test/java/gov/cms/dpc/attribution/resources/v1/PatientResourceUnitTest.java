package gov.cms.dpc.attribution.resources.v1;

import gov.cms.dpc.attribution.jdbi.*;
import gov.cms.dpc.common.entities.OrganizationEntity;
import gov.cms.dpc.common.entities.PatientEntity;
import gov.cms.dpc.common.utils.PagingService;
import gov.cms.dpc.fhir.converters.FHIREntityConverter;
import org.hl7.fhir.dstu3.model.Bundle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static gov.cms.dpc.attribution.AttributionTestHelpers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

class PatientResourceUnitTest {

    private PatientResource patientResource;

    @Mock
    PatientDAO patientDAO;

    private final FHIREntityConverter converter = FHIREntityConverter.initialize();


    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        patientResource = new PatientResource(converter, patientDAO, 9999, 100, new PagingService());
    }

    @Test
    void testSearchPatientsPaginated() {
        UUID orgId = UUID.randomUUID();
        String orgRef = "Organization/" + orgId;
        int pageOffset = 30;
        int count = 10;

        // Create 100 fake PatientEntity records
        OrganizationEntity org = createOrganizationEntity();
        List<PatientEntity> patientEntities = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            patientEntities.add(createPatientEntity(org));
        }

        // Stub DAO to return paged subset of PatientEntity's
        // if offset=30, count=10 should get patients from index 30-39
        List<PatientEntity> pagedEntities = patientEntities.subList(pageOffset, pageOffset + count);
        PageResult mockedPage = new PageResult<>(
                pagedEntities,
                true
        );
        Mockito.when(patientDAO.patientSearch(argThat(queryMatches(null, orgId, count, pageOffset))))
                .thenReturn(mockedPage);

        // this is the droid you are looking for
        Bundle resultBundle = patientResource.searchPatients(
                null,
                null,
                orgRef,
                10,
                30,
                null
        );
        List<Bundle.BundleEntryComponent> results = resultBundle.getEntry();


        assertEquals(10, results.size());
        for (int i = 0; i < results.size(); i++) {
            String expectedId = pagedEntities.get(i).getID().toString();
            String actualId = results.get(i).getResource().getId();
            assertEquals(expectedId, actualId);
        }
    }
}
