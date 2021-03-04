package gov.cms.dpc.aggregation.service;

import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.dstu3.model.StringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public final class LookBackAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(LookBackAnalyzer.class);

    private LookBackAnalyzer() {
    }

    public static OperationOutcome analyze(List<LookBackAnswer> answers, String patientID) {
        final var patientLocation = List.of(new StringType("Patient"), new StringType("id"), new StringType(patientID));

        final var failReason = analyze(answers);
        final var outcome = new OperationOutcome();
        outcome.addIssue()
                .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                .setCode(OperationOutcome.IssueType.EXCEPTION)
                .setDetails(new CodeableConcept().setText(failReason.detail))
                .setLocation(patientLocation);

        logger.info("dpcMetric=lookBackBreakdown,failReason={}",failReason.name());
        return outcome;
    }

    private static FailReason analyze(List<LookBackAnswer> answers) {
        boolean matchOrganizationNPI = answers.parallelStream().allMatch(LookBackAnswer::orgNPIMatchAnyEobNPIs);
        boolean matchProviderNPI = answers.parallelStream().allMatch(LookBackAnswer::practitionerNPIMatchAnyEobNPIs);
        if (answers.isEmpty()){
            return FailReason.NO_DATA;
        } else if (!matchOrganizationNPI || !matchProviderNPI) {
            return FailReason.NO_NPI_MATCH;
        } else {
            return FailReason.NO_DATE_MATCH;
        }
    }

    public enum FailReason {
        NO_DATA("DPC couldn't find any claims for this MBI; unable to demonstrate relationship with provider or organization"),
        NO_NPI_MATCH("DPC couldn't find a claim for this MBI from an NPI in this organization"),
        NO_DATE_MATCH("DPC couldn't find a claim within the past 18 months for this MBI from an NPI in this organization");

        public final String detail;

        FailReason(String detail) {
            this.detail = detail;
        }
    }
}
