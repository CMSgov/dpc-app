package gov.cms.dpc.attribution.resources.v1;

import gov.cms.dpc.attribution.jdbi.*;
import gov.cms.dpc.common.entities.OrganizationEntity;
import gov.cms.dpc.common.entities.PatientEntity;
import gov.cms.dpc.common.utils.PagingService;
import gov.cms.dpc.fhir.converters.FHIREntityConverter;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static gov.cms.dpc.attribution.AttributionTestHelpers.createOrganizationEntity;
import static gov.cms.dpc.attribution.AttributionTestHelpers.createPatientEntity;
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
        Mockito.when(patientDAO.patientSearch(argThat(queryMatches(null, orgId, count, pageOffset))))
                .thenReturn(pagedEntities);

        // this is the droid you are looking for
//        List<Patient> results = patientResource.searchPatients(
        List<Bundle.BundleEntryComponent> results = patientResource.searchPatients(
                null,
                null,
                orgRef,
                10,
                30
        ).getEntry();


        assertEquals(10, results.size());
        for (int i = 0; i < results.size(); i++) {
            Patient expected = new Patient();
            expected.setId(pagedEntities.get(i).getID().toString());
            assertEquals(expected.getId(), results.get(i).getId());
        }
    }

    // DUPLICATED - TODO move to util file
    private ArgumentMatcher<PatientSearchQuery> queryMatches(UUID resourceId, UUID orgId, int count, int pageOffset) {
        return query ->
                query != null &&
                        Objects.equals(query.getResourceID(), resourceId) &&
                        Objects.equals(query.getOrganizationID(), orgId) &&
                        query.getPatientMBI() == null &&
                        query.getCount() == count &&
                        query.getPageOffset() == pageOffset
                ;
    }
}
