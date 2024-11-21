package gov.cms.dpc.aggregation.service;

import gov.cms.dpc.common.utils.NPIUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Look Back answer evaluation")
public class LookBackAnswerTest {

    private final String providerNPI = NPIUtil.generateNPI();
    private final String organizationNPI = NPIUtil.generateNPI();

    @Test
    @DisplayName("Look back date matches 🥳")
    public void testDateCriteria() {
        LookBackAnswer lookBackAnswer = new LookBackAnswer(null, null, 0, null)
                .addEobBillingPeriod(YearMonth.now());

        Assertions.assertFalse(lookBackAnswer.matchDateCriteria());

        YearMonth dateTime = YearMonth.now().minusMonths(2);
        lookBackAnswer = new LookBackAnswer(null, null, 0, YearMonth.now())
                .addEobBillingPeriod(dateTime);

        Assertions.assertEquals(2, lookBackAnswer.calculatedMonthDifference());
        Assertions.assertFalse(lookBackAnswer.matchDateCriteria());

        lookBackAnswer = new LookBackAnswer(null, null, 1, YearMonth.now())
                .addEobBillingPeriod(dateTime);

        Assertions.assertFalse(lookBackAnswer.matchDateCriteria());

        lookBackAnswer = new LookBackAnswer(null, null, 2, YearMonth.now())
                .addEobBillingPeriod(dateTime);

        Assertions.assertFalse(lookBackAnswer.matchDateCriteria());

        lookBackAnswer = new LookBackAnswer(null, null, 3, YearMonth.now())
                .addEobBillingPeriod(dateTime);

        Assertions.assertTrue(lookBackAnswer.matchDateCriteria());

    }

    @Test
    @DisplayName("Match on different look back data 🤮")
    public void testDifferentNPIValues() {
        LookBackAnswer lookBackAnswer = new LookBackAnswer(null, null, 0, null);
        Assertions.assertFalse(lookBackAnswer.matchDateCriteria());
        Assertions.assertFalse(lookBackAnswer.orgMatchEob());
        Assertions.assertFalse(lookBackAnswer.orgNPIMatchAnyEobNPIs());
        Assertions.assertFalse(lookBackAnswer.practitionerMatchEob());
        Assertions.assertFalse(lookBackAnswer.practitionerNPIMatchAnyEobNPIs());

        lookBackAnswer = new LookBackAnswer(providerNPI, organizationNPI, 0, null)
                .addEobProviders(List.of(NPIUtil.generateNPI()))
                .addEobOrganization(NPIUtil.generateNPI());
        Assertions.assertFalse(lookBackAnswer.matchDateCriteria());
        Assertions.assertFalse(lookBackAnswer.orgMatchEob());
        Assertions.assertFalse(lookBackAnswer.orgNPIMatchAnyEobNPIs());
        Assertions.assertFalse(lookBackAnswer.practitionerMatchEob());
        Assertions.assertFalse(lookBackAnswer.practitionerNPIMatchAnyEobNPIs());

        lookBackAnswer = new LookBackAnswer(providerNPI, organizationNPI, 0, null)
                .addEobOrganization(providerNPI);
        Assertions.assertFalse(lookBackAnswer.orgMatchEob());
        Assertions.assertFalse(lookBackAnswer.orgNPIMatchAnyEobNPIs());
        Assertions.assertFalse(lookBackAnswer.practitionerMatchEob());
        Assertions.assertTrue(lookBackAnswer.practitionerNPIMatchAnyEobNPIs());

        lookBackAnswer = new LookBackAnswer(providerNPI, organizationNPI, 0, null)
                .addEobProviders(List.of(providerNPI));
        Assertions.assertFalse(lookBackAnswer.orgMatchEob());
        Assertions.assertFalse(lookBackAnswer.orgNPIMatchAnyEobNPIs());
        Assertions.assertTrue(lookBackAnswer.practitionerMatchEob());
        Assertions.assertTrue(lookBackAnswer.practitionerNPIMatchAnyEobNPIs());

        lookBackAnswer = new LookBackAnswer(providerNPI, organizationNPI, 0, null)
                .addEobOrganization(organizationNPI);
        Assertions.assertTrue(lookBackAnswer.orgMatchEob());
        Assertions.assertTrue(lookBackAnswer.orgNPIMatchAnyEobNPIs());
        Assertions.assertFalse(lookBackAnswer.practitionerMatchEob());
        Assertions.assertFalse(lookBackAnswer.practitionerNPIMatchAnyEobNPIs());

        lookBackAnswer = new LookBackAnswer(providerNPI, organizationNPI, 0, null)
                .addEobProviders(List.of(organizationNPI));
        Assertions.assertFalse(lookBackAnswer.orgMatchEob());
        Assertions.assertTrue(lookBackAnswer.orgNPIMatchAnyEobNPIs());
        Assertions.assertFalse(lookBackAnswer.practitionerMatchEob());
        Assertions.assertFalse(lookBackAnswer.practitionerNPIMatchAnyEobNPIs());
    }

}
