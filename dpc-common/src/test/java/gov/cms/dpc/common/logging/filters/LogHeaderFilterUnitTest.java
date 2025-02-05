package gov.cms.dpc.common.logging.filters;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LogHeaderFilterUnitTest {
	private static LogHeaderFilter logHeaderFilter;

	private static final String headerKey = "fakeHeader";

	private static final ListAppender<ILoggingEvent> listAppender = new ListAppender<>();

	@BeforeAll
	static void setup() {
		final Logger logger = (Logger) LoggerFactory.getLogger(LogHeaderFilter.class);
		logger.addAppender(listAppender);
		logHeaderFilter = new LogHeaderFilter(headerKey);
	}

	@BeforeEach
	void setupEach() {
		listAppender.start();
	}

	@AfterEach
	void afterEach() {
		listAppender.stop();
		listAppender.list.clear();
	}

	@Test
	void testLogsHeader() throws IOException {
		final String headerValue = "fakeValue,fakervalue";
		final String headerValueLogged = headerKey + "=fakeValue\\,fakervalue";

		ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
		when(requestContext.getHeaderString(headerKey)).thenReturn(headerValue);

		logHeaderFilter.filter(requestContext);

		assertEquals(1, listAppender.list.size());
		assertEquals(Level.INFO, listAppender.list.get(0).getLevel());
		assertEquals(headerValueLogged, listAppender.list.get(0).getFormattedMessage());
	}

	@Test
	void testLogsNull() throws IOException {
		final String headerValueLogged = headerKey + "=null";

		ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
		when(requestContext.getHeaderString(headerKey)).thenReturn(null);

		logHeaderFilter.filter(requestContext);

		assertEquals(1, listAppender.list.size());
		assertEquals(Level.INFO, listAppender.list.get(0).getLevel());
		assertEquals(headerValueLogged, listAppender.list.get(0).getFormattedMessage());
	}
}
