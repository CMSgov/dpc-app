package gov.cms.dpc.common.logging.filters;

import ch.qos.logback.access.common.spi.IAccessEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import io.dropwizard.logging.common.filter.FilterFactory;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

class SuccessfulHealthCheckFilterTest {
    private Filter<IAccessEvent> filter;
    private final IAccessEvent event = Mockito.mock(IAccessEvent.class);
    private final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

    @BeforeEach
    public void setUp() {
        FilterFactory<IAccessEvent> filterFactory = new SuccessfulHealthCheckFilter();
        filter = filterFactory.build();
    }

    @Test
    void test_acceptNotHealthcheck() {
        Mockito.when(event.getRequestURI()).thenReturn("/nothealthcheck");
        FilterReply reply = filter.decide(event);
        Assertions.assertEquals(FilterReply.NEUTRAL, reply);
    }

    @Test
    void test_acceptHealthcheckFail() {
        Mockito.when(event.getRequestURI()).thenReturn("/healthcheck");
        Mockito.when(event.getResponse()).thenReturn(response);
        Mockito.when(response.getStatus()).thenReturn(500);
        FilterReply reply = filter.decide(event);
        Assertions.assertEquals(FilterReply.NEUTRAL, reply);
    }

    @ParameterizedTest(name = "deny excluded uri: {0}")
    @MethodSource("gov.cms.dpc.common.logging.LoggingTestUtil#excludedUris")
    void test_denyExcludedUris(String uri) {
        Mockito.when(event.getRequestURI()).thenReturn(uri);
        Mockito.when(event.getResponse()).thenReturn(response);
        Mockito.when(response.getStatus()).thenReturn(200);

        FilterReply reply = filter.decide(event);
        Assertions.assertEquals(FilterReply.DENY, reply);
    }
}
