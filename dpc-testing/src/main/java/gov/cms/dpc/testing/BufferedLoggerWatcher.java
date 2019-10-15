package gov.cms.dpc.testing;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.slf4j.LoggerFactory;

public class BufferedLoggerWatcher implements TestWatcher {

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        // Dump all the logs
        final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        for (final ch.qos.logback.classic.Logger l : loggerContext.getLoggerList()) {
            final Appender<ILoggingEvent> appender = l.getAppender("BufferedLogger");
            if (appender != null) {
                ((RingBufferLogger) appender).dumpLogMessages();
            }
        }

    }
}
