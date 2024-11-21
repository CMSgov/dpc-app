package gov.cms.dpc.testing;

import org.apache.commons.lang3.RandomStringUtils;

public final class MBIUtil {
	/**
	 * Creates a fake MBI that matches the format defined in PatientEntity
	 * @return the MBI
	 */
	public static String generateMBI() {
		String mbi;

		mbi = RandomStringUtils.secureStrong().nextNumeric(1);
		mbi += RandomStringUtils.secureStrong().nextAlphabetic(1);
		mbi += RandomStringUtils.secureStrong().nextAlphanumeric(1);
		mbi += RandomStringUtils.secureStrong().nextNumeric(1);
		mbi += RandomStringUtils.secureStrong().nextAlphabetic(1);
		mbi += RandomStringUtils.secureStrong().nextAlphanumeric(1);
		mbi += RandomStringUtils.secureStrong().nextNumeric(1);
		mbi += RandomStringUtils.secureStrong().nextAlphabetic(2);
		mbi += RandomStringUtils.secureStrong().nextNumeric(2);

		return mbi;
	}
}
