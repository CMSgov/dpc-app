package gov.cms.dpc.aggregation.service;

import gov.cms.dpc.common.utils.NPIUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

public class LookBackAnswerTest {

    private final String providerNPI = NPIUtil.generateNPI();
    private final String organizationNPI = NPIUtil.generateNPI();

    @Test
    public void testDateCriteria() {
        LookBackAnswer lookBackAnswer = new LookBackAnswer(null, null, 0, null)
                .addEobBillingPeriod(new Date());

        Assertions.assertFalse(lookBackAnswer.matchDateCriteria());

        OffsetDateTime dateTime = OffsetDateTime.now(ZoneOffset.UTC).minusMonths(2);
        lookBackAnswer = new LookBackAnswer(null, null, 0, new Date())
                .addEobBillingPeriod(Date.from(dateTime.toInstant()));

        Assertions.assertEquals(2, lookBackAnswer.calculatedMonthDifference());
        Assertions.assertFalse(lookBackAnswer.matchDateCriteria());

        lookBackAnswer = new LookBackAnswer(null, null, 1, new Date())
                .addEobBillingPeriod(Date.from(dateTime.toInstant()));

        Assertions.assertFalse(lookBackAnswer.matchDateCriteria());

        lookBackAnswer = new LookBackAnswer(null, null, 2, new Date())
                .addEobBillingPeriod(Date.from(dateTime.toInstant()));

        Assertions.assertFalse(lookBackAnswer.matchDateCriteria());

        lookBackAnswer = new LookBackAnswer(null, null, 3, new Date())
                .addEobBillingPeriod(Date.from(dateTime.toInstant()));

        Assertions.assertTrue(lookBackAnswer.matchDateCriteria());

    }

    @Test
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
