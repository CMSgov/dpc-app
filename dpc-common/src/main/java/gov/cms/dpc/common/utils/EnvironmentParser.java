package gov.cms.dpc.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnvironmentParser {

    private static final Logger logger = LoggerFactory.getLogger(EnvironmentParser.class);

    /**
     * Gets the currently executing environment from the ENV environment variable
     * Returns 'local' if no ENV is set.
     *
     * @param applicationName - {@link String} application name for logging purposes
     * @return - {@link String} Currently executing environment, local by default
     */
    public static String getEnvironment(String applicationName) {
        String envVar = "local";
        try {
            envVar = System.getenv("ENV");
        } catch (NullPointerException e) {
            // If ENV isn't set, just ignore it.
        }

        logger.info("Starting {} Service in environment: {}", applicationName, envVar);
        return envVar;
    }
}
