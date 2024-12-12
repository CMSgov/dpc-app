package gov.cms.dpc.testing;

import org.apache.commons.lang3.RandomStringUtils;

public final class MBIUtil {
	/**
	 * Creates a fake MBI that matches the format defined in PatientEntity
	 * @return the MBI
	 */
	public static String generateMBI() {
		String mbi;
        RandomStringUtils randomStringUtils = RandomStringUtils.secure();

		mbi = randomStringUtils.nextNumeric(1);
		mbi += randomStringUtils.nextAlphabetic(1);
		mbi += randomStringUtils.nextAlphanumeric(1);
		mbi += randomStringUtils.nextNumeric(1);
		mbi += randomStringUtils.nextAlphabetic(1);
		mbi += randomStringUtils.nextAlphanumeric(1);
		mbi += randomStringUtils.nextNumeric(1);
		mbi += randomStringUtils.nextAlphabetic(2);
		mbi += randomStringUtils.nextNumeric(2);

		return mbi;
	}
}
