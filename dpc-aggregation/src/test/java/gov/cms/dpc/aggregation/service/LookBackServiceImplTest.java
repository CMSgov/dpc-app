package gov.cms.dpc.aggregation.service;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
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
import org.mockito.MockitoAnnotations;

import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;

@ExtendWith(BufferedLoggerHandler.class)
public class LookBackServiceImplTest {

    private final String providerNPI = NPIUtil.generateNPI();
    private final String careTeamNPI = NPIUtil.generateNPI();
    private final UUID orgID = UUID.randomUUID();
    private final String orgNPI = NPIUtil.generateNPI();


    private LookBackServiceImpl lookBackService;
    private ExplanationOfBenefit eob;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);
        Config config = ConfigFactory.load("testing.conf").getConfig("dpc.aggregation");
        String exportPath = config.getString("exportPath");
        OperationsConfig operationsConfig = new OperationsConfig(10, exportPath, 3, YearMonth.now());
        lookBackService = new LookBackServiceImpl(operationsConfig);
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
    }

    @Test
    public void testClaimWithinTimeFrame() {
        OffsetDateTime dateTime = OffsetDateTime.now(ZoneOffset.UTC);
        eob.getBillablePeriod().setEnd(Date.from(dateTime.toInstant()));

        LookBackAnswer result = lookBackService.getLookBackAnswer(eob, orgNPI, providerNPI, 1);
        Assertions.assertTrue(result.matchDateCriteria());

        dateTime = OffsetDateTime.now(ZoneOffset.UTC).minusMonths(1);
        eob.getBillablePeriod().setEnd(Date.from(dateTime.toInstant()));

        result = lookBackService.getLookBackAnswer(eob, orgNPI, providerNPI, 1);
        Assertions.assertFalse(result.matchDateCriteria());

        dateTime = OffsetDateTime.now(ZoneOffset.UTC).plusMonths(1);
        eob.getBillablePeriod().setEnd(Date.from(dateTime.toInstant()));

        result = lookBackService.getLookBackAnswer(eob, orgNPI, providerNPI, 1);
        Assertions.assertTrue(result.matchDateCriteria());
    }

    @Test
    public void testJobOrgMatchAnyEobNPIs() {
        OffsetDateTime dateTime = OffsetDateTime.now(ZoneOffset.UTC);
        eob.getBillablePeriod().setEnd(Date.from(dateTime.toInstant()));

        LookBackAnswer result = lookBackService.getLookBackAnswer(eob, orgNPI, NPIUtil.generateNPI(), 1);
        Assertions.assertTrue(result.orgNPIMatchAnyEobNPIs());

        result = lookBackService.getLookBackAnswer(eob, NPIUtil.generateNPI(), NPIUtil.generateNPI(), 1);
        Assertions.assertFalse(result.orgNPIMatchAnyEobNPIs());
    }

    @Test
    public void testJobProviderMatchAnyEobNPIs() {
        OffsetDateTime dateTime = OffsetDateTime.now(ZoneOffset.UTC);
        eob.getBillablePeriod().setEnd(Date.from(dateTime.toInstant()));

        LookBackAnswer result = lookBackService.getLookBackAnswer(eob, NPIUtil.generateNPI(), providerNPI, 1);
        Assertions.assertTrue(result.practitionerNPIMatchAnyEobNPIs());

        result = lookBackService.getLookBackAnswer(eob, NPIUtil.generateNPI(), NPIUtil.generateNPI(), 1);
        Assertions.assertFalse(result.orgNPIMatchAnyEobNPIs());
    }
}