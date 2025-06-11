package gov.cms.dpc.common.logging.filters;

import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.dropwizard.logging.common.filter.FilterFactory;
import org.apache.commons.lang3.StringUtils;

/**
 * Implementation of {@link FilterFactory} that ensures client_tokens are not logged when requests are made to the /Token/auth endpoint
 */
@JsonTypeName("token-filter-factory")
public class AuthRelatedLoggingFilter implements FilterFactory<IAccessEvent> {

    protected AuthRelatedLoggingFilter() {
        // Not used
    }

    @Override
    public Filter<IAccessEvent> build() {
        return new Filter<>() {
            @Override
            public FilterReply decide(IAccessEvent event) {
                if (StringUtils.containsIgnoreCase(event.getQueryString(), "client_assertion")) {
                    return FilterReply.DENY;
                }
                //Anything else, let other filters do their thing
                return FilterReply.NEUTRAL;
            }
        };
    }
}
