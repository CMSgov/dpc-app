package gov.cms.dpc.common.utils;

public final class UrlGenerator {
	public static String generateVersionUrl(int port) {
		return "http://localhost:" + port + "/v1/version";
	}
}
