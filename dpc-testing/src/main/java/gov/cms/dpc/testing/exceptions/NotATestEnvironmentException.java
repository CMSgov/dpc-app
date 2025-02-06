package gov.cms.dpc.testing.exceptions;

/**
 * Thrown when you're running a destructive test in a persisted environment
 */
public class NotATestEnvironmentException extends RuntimeException {
	public NotATestEnvironmentException(String errorMessage) {
		super(errorMessage);
	}
}
