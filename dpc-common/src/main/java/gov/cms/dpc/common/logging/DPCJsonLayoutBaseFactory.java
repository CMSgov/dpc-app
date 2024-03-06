package gov.cms.dpc.common.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.LayoutBase;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.dropwizard.logging.common.layout.DiscoverableLayoutFactory;
import io.dropwizard.logging.json.EventJsonLayoutBaseFactory;

import java.util.TimeZone;

@JsonTypeName("json-dpc")
public class DPCJsonLayoutBaseFactory extends EventJsonLayoutBaseFactory implements DiscoverableLayoutFactory<ILoggingEvent> {

    @Override
    public LayoutBase<ILoggingEvent> build(LoggerContext context, TimeZone timeZone) {
        DPCJsonLayout jsonLayout = new DPCJsonLayout(this.createDropwizardJsonFormatter(), this.createTimestampFormatter(timeZone), this.createThrowableProxyConverter(context), this.getIncludes(), this.getCustomFieldNames(), this.getAdditionalFields(), this.getIncludesMdcKeys(), this.isFlattenMdc());
        jsonLayout.setContext(context);
        return jsonLayout;
    }

}
