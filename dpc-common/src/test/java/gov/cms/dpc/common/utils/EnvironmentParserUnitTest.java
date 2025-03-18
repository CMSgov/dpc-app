package gov.cms.dpc.common.utils;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SystemStubsExtension.class)
class EnvironmentParserUnitTest {

    @SystemStub
    private EnvironmentVariables envVars;

    static final ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    static String currEnv = Optional.ofNullable(System.getenv("ENV")).orElse("local");

    @BeforeAll
    static void setUp() {
        Logger logger = (Logger) LoggerFactory.getLogger(EnvironmentParser.class);
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @BeforeEach
    void setUpEach() {
        listAppender.start();
    }

    @AfterEach
    void shutDownEach() {
        listAppender.stop();
        listAppender.list.clear();
    }

    @Test
    void getEnvironment() {
        assertEquals(currEnv, EnvironmentParser.getEnvironment("DPC"));
        assertAll(
                () -> assertEquals(1, listAppender.list.size()),
                () -> assertEquals(Level.INFO, listAppender.list.get(0).getLevel()),
                () -> assertEquals("Starting DPC Service in environment: " + currEnv, listAppender.list.get(0).getFormattedMessage())
        );
    }

    @Test
    void getEnvironment_skipLog() {
        assertEquals(currEnv, EnvironmentParser.getEnvironment("DPC", false));
        assertEquals(0, listAppender.list.size());
    }

    @Test
    void getEnvironment_withValues() {
        envVars.set("ENV", "dev");

        assertEquals("dev", EnvironmentParser.getEnvironment("DPC"));
        assertAll(
                () -> assertEquals(1, listAppender.list.size()),
                () -> assertEquals(Level.INFO, listAppender.list.get(0).getLevel()),
                () -> assertEquals("Starting DPC Service in environment: dev", listAppender.list.get(0).getFormattedMessage())
        );
    }
}
