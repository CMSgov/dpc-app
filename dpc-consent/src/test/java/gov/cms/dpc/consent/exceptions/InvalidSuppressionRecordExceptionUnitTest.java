package gov.cms.dpc.consent.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InvalidSuppressionRecordExceptionUnitTest {
	@Test
	void test_InvalidSuppressionRecordException_sets_message() {
		InvalidSuppressionRecordException exception = new InvalidSuppressionRecordException("message", "file", 1);
		assertEquals("message: file, 1", exception.getMessage());
	}

	@Test
	void test_InvalidSuppressionRecordException_sets_cause() {
		Exception cause = new Exception();
		InvalidSuppressionRecordException exception = new InvalidSuppressionRecordException("message", "file", 1, cause);
		assertSame(cause, exception.getCause());
	}
}
