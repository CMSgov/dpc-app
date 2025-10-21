package gov.cms.dpc.common.logging.filters;

import ch.qos.logback.access.common.spi.IAccessEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.dropwizard.logging.common.filter.FilterFactory;

@JsonTypeName("successful-healthcheck")
public class SuccessfulHealthCheckFilter implements FilterFactory<IAccessEvent> {
    private static final String HEALTHCHECK_URI = "/healthcheck";
    @Override
    public Filter<IAccessEvent> build() {
        return new Filter<IAccessEvent>() {
            @Override
            public FilterReply decide(IAccessEvent iAccessEvent) {
                if (iAccessEvent.getRequestURI().equals(HEALTHCHECK_URI) && iAccessEvent.getResponse().getStatus() == 200) {
                    return FilterReply.DENY;
                }
                return FilterReply.NEUTRAL;
            }
        };
    }
}
