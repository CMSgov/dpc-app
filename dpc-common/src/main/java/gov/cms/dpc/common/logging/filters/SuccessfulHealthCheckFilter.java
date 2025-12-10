package gov.cms.dpc.common.logging.filters;

import ch.qos.logback.access.common.spi.IAccessEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.dropwizard.logging.common.filter.FilterFactory;

import java.util.Set;

@JsonTypeName("successful-healthcheck")
public class SuccessfulHealthCheckFilter implements FilterFactory<IAccessEvent> {
    private static final Set<String> URIS = Set.of("/healthcheck", "/v1/version", "/api/v1/version", "/ping");
    @Override
    public Filter<IAccessEvent> build() {
        return new Filter<>() {
            @Override
            public FilterReply decide(IAccessEvent iAccessEvent) {
                if (URIS.contains(iAccessEvent.getRequestURI()) && iAccessEvent.getResponse().getStatus() == 200) {
                    return FilterReply.DENY;
                }
                return FilterReply.NEUTRAL;
            }
        };
    }
}
