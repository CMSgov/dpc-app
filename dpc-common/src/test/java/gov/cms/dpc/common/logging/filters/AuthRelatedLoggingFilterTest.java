package gov.cms.dpc.common.logging.filters;


import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import io.dropwizard.logging.common.filter.FilterFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import java.util.stream.Stream;

public class AuthRelatedLoggingFilterTest {

    private Filter<IAccessEvent> filter;
    private IAccessEvent event = Mockito.mock(IAccessEvent.class);

    @BeforeEach
    public void setUp() {
        FilterFactory<IAccessEvent> filterFactory = new AuthRelatedLoggingFilter();
        filter = filterFactory.build();
    }

    @ParameterizedTest
    @MethodSource("stringSource")
    public void testQueryString(String queryString, FilterReply expectedReply) {
        Mockito.when(event.getQueryString()).thenReturn(queryString);
        FilterReply reply = filter.decide(event);
        Assertions.assertEquals(expectedReply, reply);
    }

    private static Stream<Arguments> stringSource() {
        return Stream.of(
                Arguments.of("client_assertions=secret_token", FilterReply.DENY),
                Arguments.of("someotherstuff=stuff&client_assertions=secret_token", FilterReply.DENY),
                Arguments.of("client_assertion=secret_token", FilterReply.DENY),
                Arguments.of("someotherstuff=stuff&client_assertion=secret_token", FilterReply.DENY),
                Arguments.of("someotherstuff=stuff", FilterReply.NEUTRAL)
        );
    }
}
