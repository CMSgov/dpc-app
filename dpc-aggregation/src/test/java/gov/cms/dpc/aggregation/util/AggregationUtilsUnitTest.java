package gov.cms.dpc.aggregation.util;

import gov.cms.dpc.aggregation.engine.OutcomeReason;
import gov.cms.dpc.common.utils.GzipUtil;
import org.apache.commons.io.FileUtils;
import org.bouncycastle.jcajce.provider.digest.SHA256;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

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

	@Test
	void generateChecksum_happy_path() throws IOException {
		String testData = "testData";

		final File tempPath = FileUtils.getTempDirectory();

		// Create two test files, one compressed and one not, and get both of their checksums
		File compressedFile = File.createTempFile("test", ".ndjson.gz", tempPath);
		File unCompressedFile = File.createTempFile("test", ".ndjson", tempPath);

		FileUtils.writeByteArrayToFile(compressedFile, GzipUtil.compress(testData));
		FileUtils.writeByteArrayToFile(unCompressedFile, testData.getBytes());

		// Get checksum of uncompressed file
		FileInputStream unCompressedFIS = new FileInputStream(unCompressedFile);
		byte[] compressedChecksum = AggregationUtils.generateChecksum(compressedFile);
		byte[] unCompressedChecksum =  new SHA256.Digest().digest(unCompressedFIS.readAllBytes());

		// generateChecksum on the compressed file should match the checksum of the uncompressed file
		assertArrayEquals(unCompressedChecksum, compressedChecksum);
	}
}
