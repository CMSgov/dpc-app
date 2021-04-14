package gov.cms.dpc.aggregation.service;

import gov.cms.dpc.common.utils.NPIUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class EveryoneGetsDataLookBackServiceImplTest {

    private final LookBackService lookBackService = new EveryoneGetsDataLookBackServiceImpl();

    @Test
    public void alwaysReturnTrueFromHasClaimWithin() {
        LookBackAnswer result = lookBackService.getLookBackAnswer(null, NPIUtil.generateNPI(), NPIUtil.generateNPI(), 0L);
        Assertions.assertTrue(result.orgNPIMatchAnyEobNPIs());
        Assertions.assertTrue(result.practitionerMatchEob());
        Assertions.assertTrue(result.practitionerNPIMatchAnyEobNPIs());
        Assertions.assertTrue(result.matchDateCriteria());
        Assertions.assertTrue(result.orgMatchEob());
    }
}
