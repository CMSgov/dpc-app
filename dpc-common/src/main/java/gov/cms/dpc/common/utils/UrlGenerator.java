package gov.cms.dpc.common.utils;

public final class UrlGenerator {
	/**
	 * Generates a url to a service's /version end point if the service doesn't have an app context path.
	 * @param port The port the service runs on.
	 * @return A url that points to the service's /version endpoint.
	 */
	public static String generateVersionUrl(int port) {
		return generateVersionUrl(port, "");
	}

	/**
	 * Generates a url to a service's /version end point if the service does have an app context path.
	 * @param port The port the service runs on.
	 * @param appContextPath The context path the service runs on.
	 * @return
	 */
	public static String generateVersionUrl(int port, String appContextPath) {
		// If the path doesn't end in a "/", add one
		if(!appContextPath.matches("\\/$")) {
			appContextPath += "/";
		}
		return "http://localhost:" + port + appContextPath + "v1/version";
	}
}
