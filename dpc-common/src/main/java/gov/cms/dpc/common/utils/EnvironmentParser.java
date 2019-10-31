package gov.cms.dpc.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

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
        return getEnvironment(applicationName, true);
    }

    /**
     * Gets the currently executing environment from the ENV environment variable
     * Returns 'local' if no ENV is set.
     *
     * @param applicationName - {@link String} application name for logging purposes
     * @param logEnv          - {@code true} log environment. {@code false} skip environment logging
     * @return - {@link String} Currently executing environment, local by default
     */
    public static String getEnvironment(String applicationName, boolean logEnv) {
        final String envVar = Optional.ofNullable(System.getenv("ENV")).orElse("local");


        if (logEnv)
            logger.info("Starting {} Service in environment: {}", applicationName, envVar);

        return envVar;
    }
}
