package gov.cms.dpc.testing.exceptions;

/**
 * Thrown when you're doing something potentially career altering
 */
public class DumbEngineerException extends RuntimeException {
	public DumbEngineerException(String errorMessage) {
		super(errorMessage);
	}
}
