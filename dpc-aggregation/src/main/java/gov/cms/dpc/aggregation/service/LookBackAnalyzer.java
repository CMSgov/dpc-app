package gov.cms.dpc.aggregation.service;

import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.dstu3.model.StringType;

import java.util.List;

public final class LookBackAnalyzer {

    public static final String NO_DATA_FOR_LOOK_BACK_DETAIL = "DPC couldn't find any claims for this MBI; unable to demonstrate relationship with provider or organization";
    public static final String NO_NPI_MATCH_DETAIL = "DPC couldn't find a claim for this MBI from an NPI in this organization";
    public static final String NO_MATCHES_DETAIL = "DPC couldn't find a claim for this MBI related to an NPI in this organization, or within the past 18 months";
    public static final String NO_DATE_MATCH_DETAIL = "DPC couldn't find a claim within the past 18 months for this MBI from an NPI in this organization";

    private LookBackAnalyzer() {
    }

    public static OperationOutcome analyze(List<LookBackAnswer> answers, String patientID) {
        final var patientLocation = List.of(new StringType("Patient"), new StringType("id"), new StringType(patientID));
        final var outcome = new OperationOutcome();
        final var detail = answers.isEmpty() ? NO_DATA_FOR_LOOK_BACK_DETAIL : analyze(answers);
        outcome.addIssue()
                .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                .setCode(OperationOutcome.IssueType.EXCEPTION)
                .setDetails(new CodeableConcept().setText(detail))
                .setLocation(patientLocation);
        return outcome;
    }

    private static String analyze(List<LookBackAnswer> answers) {
        boolean matchDateCriteria = answers.parallelStream().allMatch(LookBackAnswer::matchDateCriteria);
        boolean matchOrganizationNPI = answers.parallelStream().allMatch(LookBackAnswer::orgNPIMatchAnyEobNPIs);
        boolean matchProviderNPI = answers.parallelStream().allMatch(LookBackAnswer::practitionerNPIMatchAnyEobNPIs);

        if (matchDateCriteria && !matchOrganizationNPI && !matchProviderNPI) {
            return NO_NPI_MATCH_DETAIL;
        } else if (!matchDateCriteria && !matchOrganizationNPI && !matchProviderNPI) {
            return NO_MATCHES_DETAIL;
        } else {
            return NO_DATE_MATCH_DETAIL;
        }
    }
}
