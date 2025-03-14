package gov.cms.dpc.aggregation.util;

import gov.cms.dpc.aggregation.engine.OutcomeReason;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AggregationUtilsUnitTest {

	@Test
	void toOperationOutcome_happy_path() {
		OutcomeReason outcomeReason = OutcomeReason.LOOK_BACK_NO_DATA;
		String patientId = "patientId";
		OperationOutcome.IssueType issueType = OperationOutcome.IssueType.SUPPRESSED;

		OperationOutcome operationOutcome = AggregationUtils.toOperationOutcome(outcomeReason, patientId, issueType);
		OperationOutcome.OperationOutcomeIssueComponent issue = operationOutcome.getIssueFirstRep();

		assertEquals(1, operationOutcome.getIssue().size());
		assertEquals(OutcomeReason.LOOK_BACK_NO_DATA.detail, issue.getDetails().getText());
		assertTrue(issue.hasLocation(patientId));
		assertEquals(issueType, issue.getCode());
	}

	@Test
	void toOperationOutcome_default_IssueType() {
		OutcomeReason outcomeReason = OutcomeReason.LOOK_BACK_NO_DATA;
		String patientId = "patientId";

		OperationOutcome operationOutcome = AggregationUtils.toOperationOutcome(outcomeReason, patientId);
		OperationOutcome.OperationOutcomeIssueComponent issue = operationOutcome.getIssueFirstRep();

		assertEquals(1, operationOutcome.getIssue().size());
		assertEquals(OutcomeReason.LOOK_BACK_NO_DATA.detail, issue.getDetails().getText());
		assertTrue(issue.hasLocation(patientId));
		assertEquals(OperationOutcome.IssueType.EXCEPTION, issue.getCode());
	}
}
