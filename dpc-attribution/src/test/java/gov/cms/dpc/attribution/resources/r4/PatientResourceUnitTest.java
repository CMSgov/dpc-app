package gov.cms.dpc.attribution.resources.r4;

import gov.cms.dpc.attribution.jdbi.PageResult;
import gov.cms.dpc.attribution.jdbi.PatientSearchQuery;
import gov.cms.dpc.attribution.jdbi.r4.PatientDAO;
import gov.cms.dpc.common.entities.r4.OrganizationEntity;
import gov.cms.dpc.common.entities.r4.PatientEntity;
import gov.cms.dpc.common.utils.r4.PagingService;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.converters.FHIREntityConverter;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static gov.cms.dpc.attribution.utils.r4.AttributionTestHelpers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

class PatientResourceUnitTest {

    private PatientResource patientResource;

    @Mock
    PatientDAO patientDAO;

    private final FHIREntityConverter converter = FHIREntityConverter.initialize();


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        patientResource = new PatientResource(converter, patientDAO, 9999, new PagingService());
    }

    @Test
    void testSearchPatientsPaginated() {
        String requestUrl = "/r4/Patient";
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
    void testFilteredSearchPagingLinksIncludeIdentifier() {
        UUID orgId = UUID.randomUUID();
        String mbi = "4S41C00AA00";
        String identifier = DPCIdentifierSystem.MBI.getSystem() + "|" + mbi;
        OrganizationEntity org = createOrganizationEntity();
        org.setId(orgId);
        PatientEntity patient = createPatientEntity(org);
        patient.setID(UUID.randomUUID());
        patient.setBeneficiaryID(mbi);

        // Mock only returns PageResult when all args match
        Mockito.when(patientDAO.patientSearch(argThat(query -> query != null
                && Objects.equals(query.getOrganizationID().orElse(null), orgId)
                && Objects.equals(query.getPatientMBI().orElse(null), mbi)
                && Objects.equals(query.getCount().orElse(null), 1)
                && Objects.equals(query.getPageOffset().orElse(null), 0))))
                .thenReturn(new PageResult<>(List.of(patient), false));

        Bundle bundle = patientResource.searchPatients(null, identifier, "Organization/" + orgId, 1, 0, null);

        assertEquals(1, bundle.getEntry().size());
        for (String relation : List.of(IBaseBundle.LINK_SELF, "first")) {
            String url = bundle.getLink(relation).getUrl();
            URI uri = URI.create(url);
            assertEquals("/r4/Patient", uri.getPath());
            List<String> parameters = List.of(uri.getRawQuery().split("&"));
            assertEquals(3, parameters.size());
            assertTrue(parameters.contains("_count=1"));
            assertTrue(parameters.contains("_offset=0"));
            String value = parameters.stream()
                    .filter(param -> param.startsWith("identifier="))
                    .findFirst().orElseThrow().substring("identifier=".length());
            assertTrue(value.contains("%7C"));
            assertEquals(identifier, URLDecoder.decode(value, StandardCharsets.UTF_8));
        }
        assertNull(bundle.getLink(IBaseBundle.LINK_PREV));
        assertNull(bundle.getLink(IBaseBundle.LINK_NEXT));
    }

    @Test
    void testInvalidPageNumber() {
        int count = 100;
        int pageOffset = 999900;
        String requestUrl = "/r4/Patient";
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

    @Test
    void testSummaryBundle() {
        UUID orgId = UUID.randomUUID();
        String orgRef = "Organization/" + orgId;
        PatientSearchQuery expectedQuery = new PatientSearchQuery();
        expectedQuery.setOrganizationID(orgId);
        expectedQuery.setCount(0); // because summary="count"

        Mockito.when(patientDAO.countMatchingPatients(argThat(query ->
                query.getOrganizationID().equals(expectedQuery.getOrganizationID()) &&
                        query.getCount().orElseThrow() == 0
        ))).thenReturn(99);

        Bundle summaryBundle = patientResource.searchPatients(
                null,
                null,
                orgRef,
                0,
                0,
                "count"
        );

        assertEquals(99, summaryBundle.getTotal());
        assertTrue(summaryBundle.getEntry().isEmpty());
    }
}
