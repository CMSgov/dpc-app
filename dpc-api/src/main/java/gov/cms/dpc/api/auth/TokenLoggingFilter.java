package gov.cms.dpc.api.auth;

import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.dropwizard.logging.filter.FilterFactory;

/**
 * Implementation of {@link FilterFactory} that ensures client_tokens are not logged when requests are made to the /Token/auth endpoint
 */
@JsonTypeName("token-filter-factory")
public class TokenLoggingFilter implements FilterFactory<IAccessEvent> {

    private TokenLoggingFilter() {
        // Not used
    }

    @Override
    public Filter<IAccessEvent> build() {
        return new Filter<>() {
            @Override
            public FilterReply decide(IAccessEvent event) {
                // Ensure that any auth requests are NOT directly logged
                if (event.getRequestURI().equals("/v1/Token/auth")) {
                    return FilterReply.DENY;
                }
                return FilterReply.ACCEPT;
            }
        };
    }
}
