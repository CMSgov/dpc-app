package gov.cms.dpc.common.logging;

import ch.qos.logback.classic.pattern.ThrowableHandlingConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import io.dropwizard.logging.json.EventAttribute;
import io.dropwizard.logging.json.layout.EventJsonLayout;
import io.dropwizard.logging.json.layout.JsonFormatter;
import io.dropwizard.logging.json.layout.TimestampFormatter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DPCJsonLayout extends EventJsonLayout {

    private static final String MESSAGE = "message";
    private static final String EXCEPTION = "exception";
    private static final String KEY_VALUE_SEPARATOR = "=";
    private static final String ENTRY_SEPARATOR = ",";
    private static final Pattern MBI_PATTERN = Pattern.compile("\\d[a-zA-Z][a-zA-Z0-9]\\d[a-zA-Z][a-zA-Z0-9]\\d[a-zA-Z]{2}\\d{2}");
    private static final String MBI_MASK = "***MBI?***";
    private static final String DATABASE_INFO_MASK = "**********";
    private static final Pattern PSQL_BATCH_ENTRY_EXCEPTION_PATTERN = Pattern.compile("Batch entry \\d+.*?was aborted");
    private static final Pattern PSQL_DETAIL_EXCEPTION_PATTERN = Pattern.compile("Detail:.*");

    public DPCJsonLayout(JsonFormatter jsonFormatter, TimestampFormatter timestampFormatter, ThrowableHandlingConverter throwableProxyConverter, Set<EventAttribute> includes, Map<String, String> customFieldNames, Map<String, Object> additionalFields, Set<String> includesMdcKeys, boolean flattenMdc) {
        super(jsonFormatter, timestampFormatter, throwableProxyConverter, includes, customFieldNames, additionalFields, includesMdcKeys, flattenMdc);
    }

    @Override
    protected Map<String, Object> toJsonMap(ILoggingEvent event) {
        Map<String, Object> map = super.toJsonMap(event);
        if(map.get(MESSAGE) != null){
            String maskedMessage = maskMBI(event.getFormattedMessage());
            maskedMessage = maskPSQLData(maskedMessage);
            map.put(MESSAGE, maskedMessage);
            parseJsonMessageIfPossible(map, maskedMessage);
        }
        if(map.get(EXCEPTION) != null){
            String maskedExceptionDetails = maskPSQLData(map.get(EXCEPTION).toString());
            map.put(EXCEPTION, maskedExceptionDetails);
        }
        return map;
    }

    private void parseJsonMessageIfPossible(Map<String, Object> map,  String message) {
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

    private String maskMBI(String unMaskedMessage) {
        try {
            return MBI_PATTERN.matcher(unMaskedMessage).replaceAll(MBI_MASK);
        } catch (Exception e) {
            return unMaskedMessage;
        }
    }

    private String maskPSQLData(String unMaskedMessage) {
        try {
            String newMessage = PSQL_DETAIL_EXCEPTION_PATTERN.matcher(unMaskedMessage).replaceAll(DATABASE_INFO_MASK);
            newMessage = PSQL_BATCH_ENTRY_EXCEPTION_PATTERN.matcher(newMessage).replaceAll(DATABASE_INFO_MASK);
            return newMessage;
        } catch (Exception e) {
            return unMaskedMessage;
        }
    }
}
