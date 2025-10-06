package gov.cms.dpc.common.logging.filters;

import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.SecurityContext;
import org.glassfish.jersey.internal.PropertiesDelegate;
import org.glassfish.jersey.server.ContainerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

class LogQueryFilterUnitTest {
	@BeforeEach
	void clearMDC() {
		MDC.clear();
	}

	@Test
	void canParseSingleParameter() {
		LogQueryFilter logQueryFilter = new LogQueryFilter(List.of("_type"));
		logQueryFilter.filter(createRequest("/endpoint?_type=patient"));

		Map<String, String> mdcMap = MDC.getCopyOfContextMap();
		assertEquals(1, mdcMap.size());
		assertEquals("patient", mdcMap.get("_type"));
	}

	@Test
	void canParseMultipleParameters() {
		LogQueryFilter logQueryFilter = new LogQueryFilter(List.of("_type", "_since"));
		logQueryFilter.filter(createRequest("/endpoint?_type=patient&_since=01-01-2025"));

		Map<String, String> mdcMap = MDC.getCopyOfContextMap();
		assertEquals(2, mdcMap.size());
		assertEquals("patient", mdcMap.get("_type"));
		assertEquals("01-01-2025", mdcMap.get("_since"));
	}

	@Test
	void canHandleNoParametersInUrl() {
		LogQueryFilter logQueryFilter = new LogQueryFilter(List.of("_type", "_since"));
		logQueryFilter.filter(createRequest("/endpoint"));

		assertNull(MDC.getCopyOfContextMap());
	}

	@Test
	void ignoresUnspecifiedParameters() {
		LogQueryFilter logQueryFilter = new LogQueryFilter(List.of("_type"));
		logQueryFilter.filter(createRequest("/endpoint?_type=patient&_since=01-01-2025"));

		Map<String, String> mdcMap = MDC.getCopyOfContextMap();
		assertEquals(1, mdcMap.size());
		assertEquals("patient", mdcMap.get("_type"));
	}

	@Test
	void handlesEmptyParmList() {
		LogQueryFilter logQueryFilter = new LogQueryFilter(List.of());
		logQueryFilter.filter(createRequest("/endpoint?_type=patient&_since=01-01-2025"));

		assertNull(MDC.getCopyOfContextMap());
	}

	/**
	 * Creates a {@link ContainerRequest} around the given relative path and query parameters.
	 */
	private ContainerRequest createRequest(String relativeUrl) {
		return new ContainerRequest(URI.create("https://localhost:8080"),
			URI.create(relativeUrl),
			"GET",
			mock(SecurityContext.class),
			mock(PropertiesDelegate.class),
			mock(Configuration.class)
		);
	}
}
