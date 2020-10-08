package gov.cms.dpc.aggregation.service;

import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.dstu3.model.StringType;

import java.util.List;

public final class LookBackAnalyzer {

    public static final String NO_DATA_FOR_LOOK_BACK_DETAIL = "Failed to get data for look back";
    public static final String NO_NPI_MATCH_DETAIL = "No matches on organization npi or provider npi";
    public static final String NO_MATCHES_DETAIL = "No matches on date, organization npi, and provider npi";
    public static final String NO_DATE_MATCH_DETAIL = "No matches on date";

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
