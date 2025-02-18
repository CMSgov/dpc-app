package gov.cms.dpc.testing;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom JUnit {@link org.junit.jupiter.api.extension.Extension} which overrides the default Logger and ensures that our {@link BufferedLoggerHandler} is used instead.
 * <p>
 * This means we get silent logging unless {@link BufferedLoggerHandler#testFailed(ExtensionContext, Throwable)} is called, in which case we throw everything.
 */
public class BufferedLoggerHandler implements TestWatcher, BeforeAllCallback {

    public static final String LOGGER_NAME = "BufferedLogger";
    public static final String LOGGER_PATTERN = "%-4relative [%thread] %-5level %logger{35} - %msg %n";

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        // Create the logger
        final RingBufferLogger logger = new RingBufferLogger();
        logger.setName(LOGGER_NAME);
        logger.setContext(context);

        // Pattern encoder
        final PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern(LOGGER_PATTERN);
        encoder.start();
        logger.setEncoder(encoder);

        // Start logging
        logger.start();

        final ch.qos.logback.classic.Logger root = context.getLogger(Logger.ROOT_LOGGER_NAME);
        root.detachAppender("console");
        root.addAppender(logger);
        root.setLevel(Level.TRACE);
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        // Dump all the logs
        final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        for (final ch.qos.logback.classic.Logger l : loggerContext.getLoggerList()) {
            final Appender<ILoggingEvent> appender = l.getAppender(LOGGER_NAME);
            if (appender != null) {
                ((RingBufferLogger) appender).dumpLogMessages();
            }
        }

    }
}
