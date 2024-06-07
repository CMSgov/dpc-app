package gov.cms.dpc.common.utils;

public final class UrlGenerator {
	public static String generateVersionUrl(int port) {
		return "http://localhost:" + String.valueOf(port) + "/v1/version";
	}
}
