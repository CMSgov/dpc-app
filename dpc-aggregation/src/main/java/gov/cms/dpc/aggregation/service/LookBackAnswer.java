package gov.cms.dpc.aggregation.service;

import org.apache.commons.lang3.StringUtils;

import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class LookBackAnswer {

    private final String practitionerNPI;
    private final String organizationNPI;
    private final long withinMonths;
    private final YearMonth lookBackMonth;

    private final Set<String> eobProviderNPIs = new HashSet<>();
    private String eobOrganizationNPI;
    private YearMonth billingPeriodEndDate;

    public LookBackAnswer(String practitionerNPI, String organizationNPI, long withinMonths, YearMonth lookBackMonth) {
        this.practitionerNPI = practitionerNPI;
        this.organizationNPI = organizationNPI;
        this.withinMonths = withinMonths;
        this.lookBackMonth = lookBackMonth;
    }

    public LookBackAnswer addEobOrganization(String eobOrganization) {
        eobOrganizationNPI = eobOrganization;
        return this;
    }

    public LookBackAnswer addEobProviders(Collection<String> eobProviders) {
        eobProviderNPIs.addAll(eobProviders);
        return this;
    }

    public LookBackAnswer addEobBillingPeriod(YearMonth billingPeriod) {
        billingPeriodEndDate = billingPeriod;
        return this;
    }

    public Long calculatedMonthDifference() {
        Long result = null;
        if (billingPeriodEndDate != null && lookBackMonth != null) {
            result = getMonthsDifference(billingPeriodEndDate, lookBackMonth);
        }
        return result;
    }

    public boolean matchDateCriteria() {
        boolean result = false;
        Long monthDifference = calculatedMonthDifference();
        if (monthDifference != null) {
            result = monthDifference < withinMonths;
        }
        return result;
    }

    public boolean orgNPIMatchAnyEobNPIs() {
        return orgMatchEob() || eobProviderNPIs.contains(organizationNPI);
    }

    public boolean orgMatchEob() {
        return StringUtils.isNotBlank(organizationNPI) && StringUtils.equals(organizationNPI,eobOrganizationNPI);
    }

    public boolean practitionerNPIMatchAnyEobNPIs() {
        return practitionerMatchEob() || (StringUtils.isNotBlank(practitionerNPI) && StringUtils.equals(practitionerNPI,eobOrganizationNPI));
    }

    public boolean practitionerMatchEob() {
        return eobProviderNPIs.contains(practitionerNPI);
    }

    private long getMonthsDifference(YearMonth date1, YearMonth date2) {
        return ChronoUnit.MONTHS.between(date1, date2);
    }
}
