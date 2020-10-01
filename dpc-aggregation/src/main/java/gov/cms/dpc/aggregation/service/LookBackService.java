package gov.cms.dpc.aggregation.service;

import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.dstu3.model.StringType;

import java.util.List;
import java.util.UUID;

public interface LookBackService {

    /**
     * Retrieves the ProviderID from the roster by orgID, patientMBI and either rosterID or providerID
     * @param orgID         The organizationID
     * @param providerOrRosterID   Either a rosterID or the providerID
     * @param patientMBI    The patient MBI
     * @return the providerNPI for that roster
     */
    String getPractitionerNPIFromRoster(UUID orgID, String providerOrRosterID, String patientMBI);

    /**
     * Checks to see if the explanation of benefits that is associated with the orgID and providerID has a claim
     * within the last withinMonths
     * @param explanationOfBenefit  The EoB
     * @param organizationID        The organizationID
     * @param practitionerNPI           The providerNPI
     * @param withinMonth           The limit of months to qualify for having a claim
     * @return true or false if the EoB matches the organizationID and providerID and has a claim within certain months
     */
    LookBackAnswer getLookBackAnswer(ExplanationOfBenefit explanationOfBenefit, UUID organizationID, String practitionerNPI, long withinMonth);

    static OperationOutcome getOperationOutcome(List<LookBackAnswer> answers, String patientID) {
        final var patientLocation = List.of(new StringType("Patient"), new StringType("id"), new StringType(patientID));
        final var outcome = new OperationOutcome();
        final var detail = answers.isEmpty() ? "Failed to get data for look back" : "Failed look back";
        outcome.addIssue()
                .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                .setCode(OperationOutcome.IssueType.EXCEPTION)
                .setDetails(new CodeableConcept().setText(detail))
                .setLocation(patientLocation);
        return outcome;
    }
}
