package gov.cms.dpc.attribution.resources.v1;

import gov.cms.dpc.attribution.jdbi.*;
import gov.cms.dpc.common.entities.OrganizationEntity;
import gov.cms.dpc.common.entities.PatientEntity;
import gov.cms.dpc.common.utils.PagingService;
import gov.cms.dpc.fhir.converters.FHIREntityConverter;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.instance.model.api.IBaseBundle;
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
        patientResource = new PatientResource(converter, patientDAO, 9999, new PagingService("http://localhost:3002"));
    }

    @Test
    void testSearchPatientsPaginated() {
        String requestUrl = "http://localhost:3002/v1/Patient";
        UUID orgId = UUID.randomUUID();
        String orgRef = "Organization/" + orgId;
        int pageOffset = 30;
        int count = 10;

        // Create 100 fake PatientEntity records
        OrganizationEntity org = createOrganizationEntity();
        List<PatientEntity> patientEntities = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            PatientEntity patientEntity = createPatientEntity(org);
            patientEntity.setID(UUID.randomUUID());
            patientEntities.add(patientEntity);
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
                ""
        );
        List<Bundle.BundleEntryComponent> results = resultBundle.getEntry();


        assertEquals(10, results.size());
        for (int i = 0; i < results.size(); i++) {
            String expectedId = pagedEntities.get(i).getID().toString();
            String actualId = results.get(i).getResource().getId();
            assertEquals(expectedId, actualId);
        }
        String expectedPrevUrl = requestUrl + "?_count=10&_offset=20";
        String expectedNextUrl = requestUrl + "?_count=10&_offset=40";
        String expectedFirstUrl = requestUrl + "?_count=10&_offset=0";
        String expectedSelfUrl = requestUrl + "?_count=10&_offset=30";
        assertEquals(expectedPrevUrl, resultBundle.getLink(IBaseBundle.LINK_PREV).getUrl());
        assertEquals(expectedNextUrl, resultBundle.getLink(IBaseBundle.LINK_NEXT).getUrl());
        assertEquals(expectedFirstUrl, resultBundle.getLink("first").getUrl());
        assertEquals(expectedSelfUrl, resultBundle.getLink(IBaseBundle.LINK_SELF).getUrl());
        assertEquals(count, resultBundle.getEntry().size());
    }

    @Test
    void testInvalidPageNumber() {
        int count = 100;
        int pageOffset = 999900;
        String requestUrl = "http://localhost:3002/v1/Patient";
        UUID orgId = UUID.randomUUID();
        String orgRef = "Organization/" + orgId;

        List<PatientEntity> patientEntities = new ArrayList<>();
        PageResult mockedPage = new PageResult<>(
                patientEntities,
                false
        );
        Mockito.when(patientDAO.patientSearch(argThat(queryMatches(null, orgId, count, pageOffset))))
                .thenReturn(mockedPage);
        Bundle resultBundle = patientResource.searchPatients(
                null,
                null,
                orgRef,
                count,
                pageOffset,
                ""
        );

        assertNull(resultBundle.getLink(IBaseBundle.LINK_PREV));
        assertNull(resultBundle.getLink(IBaseBundle.LINK_NEXT));
        assertEquals(resultBundle.getLink("first").getUrl(), requestUrl + "?_count=100&_offset=0");
        assertEquals(resultBundle.getLink(IBaseBundle.LINK_SELF).getUrl(), requestUrl + "?_count=" + count + "&_offset=" + pageOffset);
        assertTrue(resultBundle.getEntry().isEmpty(), "Expected no entries for an out-of-bounds page");
    }
}
