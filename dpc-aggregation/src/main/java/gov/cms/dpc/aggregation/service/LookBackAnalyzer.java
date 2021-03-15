package gov.cms.dpc.aggregation.service;

import gov.cms.dpc.aggregation.engine.OutcomeReason;

import java.util.List;

public final class LookBackAnalyzer {

    private LookBackAnalyzer() {
    }

    public static OutcomeReason analyze(List<LookBackAnswer> answers) {
        boolean matchOrganizationNPI = answers.parallelStream().allMatch(LookBackAnswer::orgNPIMatchAnyEobNPIs);
        boolean matchProviderNPI = answers.parallelStream().allMatch(LookBackAnswer::practitionerNPIMatchAnyEobNPIs);
        if (answers.isEmpty()){
            return OutcomeReason.LOOK_BACK_NO_DATA;
        } else if (!matchOrganizationNPI || !matchProviderNPI) {
            return OutcomeReason.LOOK_BACK_NO_NPI_MATCH;
        } else {
            return OutcomeReason.LOOK_BACK_NO_DATE_MATCH;
        }
    }


}
