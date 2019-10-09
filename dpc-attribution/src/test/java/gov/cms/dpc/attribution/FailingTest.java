package gov.cms.dpc.attribution;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import gov.cms.dpc.common.logging.RingBufferLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(FailingTest.FailWatcher.class)
class FailingTest {
    private static final Logger logger = LoggerFactory.getLogger(FailingTest.class);

    @Test
    void testFailure() {
//        final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
//        final ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger("");
//        rootLogger.addAppender(new RingBufferLogger());
        FailingTest.logger.info("I'm logging away");
        logger.error("Error!", new IllegalArgumentException("Nope"));
        assertFalse(true, "Should always fail");
    }

    @Test
    void testSuccess() {
//        final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
//        final ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger("");
//        rootLogger.addAppender(new RingBufferLogger());
        FailingTest.logger.info("I'm logging away");
//        logger.error("Error!", new IllegalArgumentException("Nope"));
        assertTrue(true, "Should always fail");
    }

    public static class FailWatcher implements TestWatcher {

        @Override
        public void testFailed(ExtensionContext context, Throwable cause) {
            context.getDisplayName();
            // Dump all the logs
            final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

            for (final ch.qos.logback.classic.Logger l : loggerContext.getLoggerList()) {
                final Appender <ILoggingEvent> appender = l.getAppender("buffer");
                if (appender != null) {
                    ((RingBufferLogger) appender).dumpLogMessages();
                }
            }

        }
    }
}
