package gov.cms.dpc.aggregation.service;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import gov.cms.dpc.aggregation.dao.OrganizationDAO;
import gov.cms.dpc.aggregation.dao.RosterDAO;
import gov.cms.dpc.aggregation.engine.OperationsConfig;
import gov.cms.dpc.common.utils.NPIUtil;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Reference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(BufferedLoggerHandler.class)
public class LookBackServiceImplTest {

    private String providerNPI = NPIUtil.generateNPI();
    private String careTeamNPI = NPIUtil.generateNPI();
    private UUID orgID = UUID.randomUUID();
    private String orgNPI = NPIUtil.generateNPI();


    private LookBackServiceImpl lookBackService;
    private ExplanationOfBenefit eob;

    @Mock
    private RosterDAO rosterDAO;

    @Mock
    private OrganizationDAO organizationDAO;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.initMocks(this);
        Config config = ConfigFactory.load("testing.conf").getConfig("dpc.aggregation");
        String exportPath = config.getString("exportPath");
        OperationsConfig operationsConfig = new OperationsConfig(10, exportPath, 3, new Date());
        lookBackService = new LookBackServiceImpl(rosterDAO, organizationDAO, operationsConfig);
        eob = new ExplanationOfBenefit();
        eob.setBillablePeriod(new Period());
        eob.setProvider(new Reference());
        eob.getProvider().getIdentifier().setSystem(DPCIdentifierSystem.NPPES.getSystem());
        eob.getProvider().getIdentifier().setValue(providerNPI);
        eob.setOrganization(new Reference());
        eob.getOrganization().getIdentifier().setSystem(DPCIdentifierSystem.NPPES.getSystem());
        eob.getOrganization().getIdentifier().setValue(orgNPI);
        eob.getOrganization().setId(orgID.toString());
        eob.getCareTeamFirstRep().getProvider().getIdentifier().setSystem(DPCIdentifierSystem.NPPES.getSystem());
        eob.getCareTeamFirstRep().getProvider().getIdentifier().setValue(careTeamNPI);

        Mockito.when(organizationDAO.fetchOrganizationNPI(orgID)).thenReturn(Optional.of(orgNPI));
    }

    @Test
    public void testClaimWithinTimeFrame() {
        OffsetDateTime dateTime = OffsetDateTime.now(ZoneOffset.UTC);
        eob.getBillablePeriod().setEnd(Date.from(dateTime.toInstant()));

        boolean result = lookBackService.hasClaimWithin(eob, orgID, providerNPI, 1);
        Assertions.assertTrue(result);

        dateTime = OffsetDateTime.now(ZoneOffset.UTC).minusMonths(1);
        eob.getBillablePeriod().setEnd(Date.from(dateTime.toInstant()));

        result = lookBackService.hasClaimWithin(eob, orgID, providerNPI, 1);
        Assertions.assertFalse(result);

        dateTime = OffsetDateTime.now(ZoneOffset.UTC).plusMonths(1);
        eob.getBillablePeriod().setEnd(Date.from(dateTime.toInstant()));

        result = lookBackService.hasClaimWithin(eob, orgID, providerNPI, 1);
        Assertions.assertTrue(result);
    }

    @Test
    public void testJobOrgMatchAnyEobNPIs() {
        OffsetDateTime dateTime = OffsetDateTime.now(ZoneOffset.UTC);
        eob.getBillablePeriod().setEnd(Date.from(dateTime.toInstant()));

        boolean result = lookBackService.hasClaimWithin(eob, orgID, NPIUtil.generateNPI(), 1);
        Assertions.assertTrue(result);

        result = lookBackService.hasClaimWithin(eob, UUID.randomUUID(), NPIUtil.generateNPI(), 1);
        Assertions.assertFalse(result);
    }

    @Test
    public void testJobProviderMatchAnyEobNPIs() {
        OffsetDateTime dateTime = OffsetDateTime.now(ZoneOffset.UTC);
        eob.getBillablePeriod().setEnd(Date.from(dateTime.toInstant()));

        boolean result = lookBackService.hasClaimWithin(eob, UUID.randomUUID(), providerNPI, 1);
        Assertions.assertTrue(result);

        result = lookBackService.hasClaimWithin(eob, UUID.randomUUID(), NPIUtil.generateNPI(), 1);
        Assertions.assertFalse(result);
    }
}