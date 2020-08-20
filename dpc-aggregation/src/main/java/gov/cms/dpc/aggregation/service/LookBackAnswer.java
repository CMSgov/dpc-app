package gov.cms.dpc.aggregation.service;

public class LookBackAnswer {

    private boolean matchLookBackLimitCriteria;
    private boolean matchProvidersCriteria;
    private boolean matchOrganizationCriteria;
    private long billingDateMonthsFromNow;

    public boolean isMatchLookBackLimitCriteria() {
        return matchLookBackLimitCriteria;
    }

    public void setMatchLookBackLimitCriteria(boolean matchLookBackLimitCriteria) {
        this.matchLookBackLimitCriteria = matchLookBackLimitCriteria;
    }

    public boolean isMatchProvidersCriteria() {
        return matchProvidersCriteria;
    }

    public void setMatchProvidersCriteria(boolean matchProvidersCriteria) {
        this.matchProvidersCriteria = matchProvidersCriteria;
    }

    public boolean isMatchOrganizationCriteria() {
        return matchOrganizationCriteria;
    }

    public void setMatchOrganizationCriteria(boolean matchOrganizationCriteria) {
        this.matchOrganizationCriteria = matchOrganizationCriteria;
    }

    public long getBillingDateMonthsFromNow() {
        return billingDateMonthsFromNow;
    }

    public void setBillingDateMonthsFromNow(long billingDateMonthsFromNow) {
        this.billingDateMonthsFromNow = billingDateMonthsFromNow;
    }

    public boolean answer() {
        return matchLookBackLimitCriteria && matchOrganizationCriteria && matchProvidersCriteria;
    }
}
