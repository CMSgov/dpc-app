package gov.cms.dpc.common.logging.filters;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.DisplayName;

@ExtendWith(SystemStubsExtension.class)
@DisplayName("Secret logging filter")
class SecretLoggingFilterUnitTest {

	@SystemStub
	private EnvironmentVariables envVars;
	@Test
        @DisplayName("Deny log entry 🥳")
	void test_canDenyLogs() {
		envVars.set("TEST_SECRET", "test_secret");

		SecretLoggingFilter filterFactory = new SecretLoggingFilter();
		filterFactory.setSecrets(List.of("TEST_SECRET"));
		Filter<ILoggingEvent> filter = filterFactory.build();

		LoggingEvent logEvent = new LoggingEvent();
		logEvent.setMessage("test_secret");

		assertEquals(FilterReply.DENY, filter.decide(logEvent));
	}

	@Test
        @DisplayName("Accept log entry 🥳")
	void test_canAcceptLogs() {
		envVars.set("TEST_SECRET", "test_secret");

		SecretLoggingFilter filterFactory = new SecretLoggingFilter();
		filterFactory.setSecrets(List.of("TEST_SECRET"));
		Filter<ILoggingEvent> filter = filterFactory.build();

		LoggingEvent logEvent = new LoggingEvent();
		logEvent.setMessage("blah blah");

		assertEquals(FilterReply.NEUTRAL, filter.decide(logEvent));
	}

	@Test
        @DisplayName("Accept logging with missing secret 🥳")
	void test_canHandleMissingSecrets() {
		envVars.remove("NON_EXISTENT_SECRET");

		SecretLoggingFilter filterFactory = new SecretLoggingFilter();

		List<String> secrets = new ArrayList<>();
		secrets.add("NON_EXISTENT_SECRET");
		filterFactory.setSecrets(secrets);
		Filter<ILoggingEvent> filter = filterFactory.build();

		LoggingEvent logEvent = new LoggingEvent();
		logEvent.setMessage("blah blah");

		assertEquals(FilterReply.NEUTRAL, filter.decide(logEvent));
	}
}
