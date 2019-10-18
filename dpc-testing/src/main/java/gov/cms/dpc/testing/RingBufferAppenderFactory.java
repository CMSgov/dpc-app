package gov.cms.dpc.testing;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.OutputStreamAppender;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.dropwizard.logging.AbstractOutputStreamAppenderFactory;

@JsonTypeName("ringBuffer")
public class RingBufferAppenderFactory extends AbstractOutputStreamAppenderFactory<ILoggingEvent> {

    @Override
    protected OutputStreamAppender<ILoggingEvent> appender(LoggerContext loggerContext) {
        return RingBufferLoggerInstance.buildInstance(loggerContext);
    }
}
