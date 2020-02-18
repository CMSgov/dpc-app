package gov.cms.dpc.common.logging;

import ch.qos.logback.classic.pattern.ThrowableHandlingConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import io.dropwizard.logging.json.EventAttribute;
import io.dropwizard.logging.json.layout.EventJsonLayout;
import io.dropwizard.logging.json.layout.JsonFormatter;
import io.dropwizard.logging.json.layout.TimestampFormatter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DPCJsonLayout extends EventJsonLayout {

    private static final String MESSAGE = "message";
    private static final String KEY_VALUE_SEPARATOR = "=";
    private static final String ENTRY_SEPARATOR = ",";

    public DPCJsonLayout(JsonFormatter jsonFormatter, TimestampFormatter timestampFormatter, ThrowableHandlingConverter throwableProxyConverter, Set<EventAttribute> includes, Map<String, String> customFieldNames, Map<String, Object> additionalFields, Set<String> includesMdcKeys, boolean flattenMdc) {
        super(jsonFormatter, timestampFormatter, throwableProxyConverter, includes, customFieldNames, additionalFields, includesMdcKeys, flattenMdc);
    }

    @Override
    protected Map<String, Object> toJsonMap(ILoggingEvent event) {
        Map<String, Object> map = super.toJsonMap(event);
        parseJsonMessageIfPossible(map, event);
        return map;
    }

    private void parseJsonMessageIfPossible(Map<String, Object> map, ILoggingEvent event) {
        if (!map.containsKey(MESSAGE)) {
            return;
        }

        String message = event.getFormattedMessage();
        try {
            Map<String, String> mappedMessage = splitToMap(message);
            map.remove(MESSAGE);
            map.putAll(mappedMessage);
        } catch (Exception e) {
            //do nothing, leave original map alone
        }
    }

    private Map<String, String> splitToMap(String in) {
        return Arrays.stream(in.split(ENTRY_SEPARATOR))
                .map(s -> s.split(KEY_VALUE_SEPARATOR))
                .collect(Collectors.toMap(
                        a -> StringUtils.strip(a[0]),  //key
                        a -> StringUtils.strip(a[1])   //value
                ));
    }

}
