package gov.cms.dpc.common.logging.filters;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import java.io.IOException;

import static org.mockito.Mockito.*;

class LogHeaderFilterUnitTest {
	@Test
	public void testLogsHeader() throws IOException {
		final String headerKey = "fakeHeader";
		final String headerValue = "fakeValue";

		Logger logger = mock(Logger.class);

		ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
		when(requestContext.getHeaderString(headerKey)).thenReturn(headerValue);

		try (MockedStatic<LoggerFactory> loggerFactory = mockStatic(LoggerFactory.class)) {
			loggerFactory.when(() -> LoggerFactory.getLogger(LogHeaderFilter.class))
				.thenReturn(logger);

			LogHeaderFilter logHeaderFilter = new LogHeaderFilter(headerKey);
			logHeaderFilter.filter(requestContext);

			// Verify that our mock logger was called with the correct values
			verify(logger).info(Mockito.anyString(), eq(headerKey), eq(headerValue));
		}
	}
}
