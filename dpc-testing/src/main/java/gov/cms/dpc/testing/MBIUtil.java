package gov.cms.dpc.testing;

import org.apache.commons.lang3.RandomStringUtils;

public final class MBIUtil {
	/**
	 * Creates a fake MBI that matches the format defined in PatientEntity
	 * @return the MBI
	 */
	public static String generateMBI() {
		String mbi;
        RandomStringUtils random = RandomStringUtils.secure();

		mbi = random.nextNumeric(1);
		mbi += random.nextAlphabetic(1);
		mbi += random.nextAlphanumeric(1);
		mbi += random.nextNumeric(1);
		mbi += random.nextAlphabetic(1);
		mbi += random.nextAlphanumeric(1);
		mbi += random.nextNumeric(1);
		mbi += random.nextAlphabetic(2);
		mbi += random.nextNumeric(2);

		return mbi;
	}
}
