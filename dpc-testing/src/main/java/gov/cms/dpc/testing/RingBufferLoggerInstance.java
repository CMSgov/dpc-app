package gov.cms.dpc.testing;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

class RingBufferLoggerInstance {

    private static final String LOGGER_NAME = "BufferedLogger";
    private static final String LOGGER_PATTERN = "%-4relative [%thread] %-5level %logger{35} - %msg %n";

    private static Map<LoggerContext, RingBufferLogger> instances = new HashMap<>();

    static synchronized RingBufferLogger buildInstance(LoggerContext loggerContext) {
        if ( instances.containsKey(loggerContext) ) {
            return instances.get(loggerContext);
        }

        final RingBufferLogger logger = new RingBufferLogger();
        logger.setName(LOGGER_NAME);
        logger.setContext(loggerContext);

        // Pattern encoder
        final PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern(LOGGER_PATTERN);
        encoder.start();
        logger.setEncoder(encoder);

        // Start logging
        logger.start();

        final ch.qos.logback.classic.Logger root = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        root.detachAppender("console");
        root.addAppender(logger);
        root.setLevel(Level.TRACE);

        return logger;
    }

    static void dumpLogMessages() {
        instances.values().forEach(RingBufferLogger::dumpLogMessages);
    }

    static synchronized void destroyInstance(LoggerContext loggerContext) {
        instances.remove(loggerContext);
    }

}
