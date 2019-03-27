package gov.cms.dpc.attribution.jobs;

import gov.cms.dpc.attribution.jdbi.RelationshipDAO;
import gov.cms.dpc.attribution.models.AttributionRelationship;
import gov.cms.dpc.attribution.models.PatientEntity;
import gov.cms.dpc.attribution.models.ProviderEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public class AttributionExpirationTest {

    private RelationshipDAO dao = Mockito.mock(RelationshipDAO.class);
    private ExpireAttributions job;

    @BeforeEach
    public void setup() {
        Mockito.reset(dao);

        // Setup the mock calls
        setupMocks();

        job = new ExpireAttributions(dao, Duration.ofDays(10));
    }

    @Test
    public void simpleExpirationTest() {
        this.job.doRun();

        // Verify that remove was called once
        Mockito.verify(dao, Mockito
                .times(1))
                .removeAttributionRelationship(Mockito.any());
    }

    private void setupMocks() {

        Mockito.when(dao.getAttributions())
                .thenReturn(generateTestRelationships());
    }

    private List<AttributionRelationship> generateTestRelationships() {
        // Create some attribution relationships
        List<AttributionRelationship> testRelationships = new ArrayList<>();

        testRelationships.add(new AttributionRelationship(new ProviderEntity(), new PatientEntity(), LocalDate.of(2017, 3, 11).atStartOfDay().atOffset(ZoneOffset.UTC)));
        testRelationships.add(new AttributionRelationship(new ProviderEntity(), new PatientEntity(), OffsetDateTime.now()));

        return testRelationships;
    }

}
