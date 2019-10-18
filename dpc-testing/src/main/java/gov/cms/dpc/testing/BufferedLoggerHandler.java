package gov.cms.dpc.testing;

import ch.qos.logback.classic.LoggerContext;
import org.junit.jupiter.api.extension.*;
import org.slf4j.LoggerFactory;

/**
 * Custom JUnit {@link org.junit.jupiter.api.extension.Extension} which overrides the default Logger and ensures that our {@link BufferedLoggerHandler} is used instead.
 * <p>
 * This means we get silent logging unless {@link BufferedLoggerHandler#testFailed(ExtensionContext, Throwable)} is called, in which case we throw everything.
 */
public class BufferedLoggerHandler implements TestWatcher, BeforeAllCallback, AfterAllCallback, BeforeEachCallback {

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        RingBufferLoggerInstance.buildInstance(context);
        context.getLoggerList();
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getLoggerList();
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        // Dump all the logs
        RingBufferLoggerInstance.dumpLogMessages();
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        RingBufferLoggerInstance.destroyInstance(context);
    }
}
