package gov.cms.dpc.common.logging;

import ch.qos.logback.classic.pattern.ThrowableHandlingConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggerContextVO;
import io.dropwizard.logging.json.EventAttribute;
import io.dropwizard.logging.json.layout.JsonFormatter;
import io.dropwizard.logging.json.layout.TimestampFormatter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DPCJsonLayoutTest {

    private DPCJsonLayout dpcJsonLayout;

    @Mock
    private ILoggingEvent loggingEvent;

    @Mock
    private LoggerContextVO loggerContextVO;

    @Mock
    private JsonFormatter jsonFormatter;

    @Mock
    private TimestampFormatter timestampFormatter;

    @Mock
    private ThrowableHandlingConverter throwableHandlingConverter;

    @Before
    public void setup() {
        dpcJsonLayout = new DPCJsonLayout(jsonFormatter,
                timestampFormatter,
                throwableHandlingConverter,
                Set.of(EventAttribute.MESSAGE),
                new HashMap<String, String>(),
                new HashMap<String, Object>(),
                new HashSet<>(),
                false
                );
        when(loggerContextVO.getName()).thenReturn("name");
        when(loggingEvent.getLoggerContextVO()).thenReturn(loggerContextVO);
    }

    @Test
    public void noChangeWhenMessageNotParseableAsMap() {
        String message = "hello I'm not parsable";

        when(loggingEvent.getFormattedMessage()).thenReturn(message);
        Map<String, Object> map = dpcJsonLayout.toJsonMap(loggingEvent);
        Assert.assertEquals(message, map.get("message"));
    }

    @Test
    public void changeWhenMessageIsParsableAsMap() {
        String message = "key1=value1, key2=value2";

        when(loggingEvent.getFormattedMessage()).thenReturn(message);
        Map<String, Object> map = dpcJsonLayout.toJsonMap(loggingEvent);
        Assert.assertFalse(map.containsKey("message"));
        Assert.assertEquals("value1", map.get("key1"));
        Assert.assertEquals("value2", map.get("key2"));
    }
}
