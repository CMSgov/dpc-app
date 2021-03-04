package gov.cms.dpc.aggregation.service;

import gov.cms.dpc.aggregation.engine.OutcomeReason;
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

        logger.info("dpcMetric=OperationOutcomeReason,failReason={}",failReason.name());
        return outcome;
    }

    private static OutcomeReason analyze(List<LookBackAnswer> answers) {
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
