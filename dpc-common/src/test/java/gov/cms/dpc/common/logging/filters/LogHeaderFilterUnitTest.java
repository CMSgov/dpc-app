package gov.cms.dpc.common.logging.filters;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.*;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;

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
        final String fakeUrl = "fake/fake/path";
		final String fakeHeaderValueAdded = headerKey + "=fakeValue\\,fakervalue";
        final String fakeUriValueAdded = "uri=" + fakeUrl;
        final String headerValueLogged = fakeHeaderValueAdded + ", " + fakeUriValueAdded;

		ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
		when(requestContext.getHeaderString(headerKey)).thenReturn(headerValue);
        final URI fakeUri = URI.create(fakeUrl);
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getRequestUri()).thenReturn(fakeUri);
        when(requestContext.getUriInfo()).thenReturn(uriInfo);

		logHeaderFilter.filter(requestContext);

		assertEquals(1, listAppender.list.size());
		assertEquals(Level.INFO, listAppender.list.get(0).getLevel());
		assertEquals(headerValueLogged, listAppender.list.get(0).getFormattedMessage());
	}

    @Test
    void testLogMessageSanitization() throws IOException {
        final String headerValue = "fakeValue,fakervalue";
        final String fakeHeaderValueAdded = headerKey + "=fakeValue\\,fakervalue";
        final String fakeUriValueSanitized = "uri=" + "/api/v1/Group?user=attacker_admin=true";
        final String expectedLogMessage = fakeHeaderValueAdded + ", " + fakeUriValueSanitized;


        final URI fakeUri = mock(URI.class);
        UriInfo uriInfo = mock(UriInfo.class);
        when(fakeUri.toString()).thenReturn("/api/v1/Group?user=attacker\nadmin=true");
        when(uriInfo.getRequestUri()).thenReturn(fakeUri);

        ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        when(requestContext.getHeaderString(headerKey)).thenReturn(headerValue);
        when(requestContext.getUriInfo()).thenReturn(uriInfo);

        logHeaderFilter.filter(requestContext);

        assertEquals(1, listAppender.list.size());
        assertEquals(Level.INFO, listAppender.list.get(0).getLevel());
        assertEquals(expectedLogMessage, listAppender.list.get(0).getFormattedMessage());
    }

	@Test
	void testLogsNull() throws IOException {
		final String headerValueLogged = headerKey + "=null, uri=null";

		ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
		when(requestContext.getHeaderString(headerKey)).thenReturn(null);

		logHeaderFilter.filter(requestContext);

		assertEquals(1, listAppender.list.size());
		assertEquals(Level.INFO, listAppender.list.get(0).getLevel());
		assertEquals(headerValueLogged, listAppender.list.get(0).getFormattedMessage());
	}
}
