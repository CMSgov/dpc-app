package gov.cms.dpc.testing;

import org.apache.commons.lang3.RandomStringUtils;

public final class MBIUtil {
	// Creates a fake MBI that matches the format defined in our PatientEntity

	/**
	 * Creates a fake MBI that matches the format defined in PatientEntity
	 * @return the MBI
	 */
	public static String generateMBI() {
		String mbi;

		mbi = RandomStringUtils.randomNumeric(1);
		mbi += RandomStringUtils.randomAlphabetic(1);
		mbi += RandomStringUtils.randomAlphanumeric(1);
		mbi += RandomStringUtils.randomNumeric(1);
		mbi += RandomStringUtils.randomAlphabetic(1);
		mbi += RandomStringUtils.randomAlphanumeric(1);
		mbi += RandomStringUtils.randomNumeric(1);
		mbi += RandomStringUtils.randomAlphabetic(2);
		mbi += RandomStringUtils.randomNumeric(2);

		return mbi;
	}
}
