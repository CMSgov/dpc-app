package gov.cms.dpc.common.logging.filters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import javax.ws.rs.container.ContainerRequestContext;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.mockito.Mockito.*;

class LogHeaderFilterUnitTest {

	private final Logger logger = mock(Logger.class);

	private LogHeaderFilter logHeaderFilter;

	private final String headerKey = "fakeHeader";
	private final String logFormat = "{}={}";

	@BeforeEach
	public void setup() throws NoSuchFieldException, IllegalAccessException {
		// There isn't really a non-hacky way to mock a static final field, but this
		// feels like the best of a bunch of bad options.

		// Grab the logger field in LogHeaderFilter and make it accessible
		Field field = LogHeaderFilter.class.getDeclaredField("logger");
		field.setAccessible(true);

		Field modifiersField = Field.class.getDeclaredField("modifiers");
		modifiersField.setAccessible(true);
		modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

		// Set the logger to our mock
		field.set(null, logger);
		logHeaderFilter = new LogHeaderFilter("fakeHeader");
	}

	@Test
	public void testLogsHeader() throws IOException {
		final String headerValue = "fakeValue,fakervalue";
		final String headerValueLogged = "\"fakeValue,fakervalue\"";

		ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
		when(requestContext.getHeaderString(headerKey)).thenReturn(headerValue);

		logHeaderFilter.filter(requestContext);

		// Verify that our mock logger was called with the correct values
		verify(logger).info(logFormat, headerKey, headerValueLogged);
	}

	@Test
	public void testLogsNull() throws IOException {
		ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
		when(requestContext.getHeaderString(headerKey)).thenReturn(null);

		logHeaderFilter.filter(requestContext);

		// Verify that our mock logger was called with the correct values
		verify(logger).info(logFormat, headerKey, null);
	}
}
