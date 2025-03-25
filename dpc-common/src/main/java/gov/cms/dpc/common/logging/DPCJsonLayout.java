package gov.cms.dpc.common.logging;

import ch.qos.logback.classic.pattern.ThrowableHandlingConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import io.dropwizard.logging.json.EventAttribute;
import io.dropwizard.logging.json.layout.EventJsonLayout;
import io.dropwizard.logging.json.layout.JsonFormatter;
import io.dropwizard.logging.json.layout.TimestampFormatter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class DPCJsonLayout extends EventJsonLayout {

    private static final String MESSAGE = "message";
    private static final String EXCEPTION = "exception";
    private static final String KEY_VALUE_SEPARATOR = "=";

    private static final Character ENTRY_SEPARATOR = ',';
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
        map.computeIfPresent(EXCEPTION, (exc, unmasked) -> maskPSQLData(unmasked.toString()));
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

    private Map<String, String> splitToMap(String in) throws IOException {
        // Allow the usage of '\' as an escape character so we can log values with a comma.
        CSVFormat csvFormat = CSVFormat.newFormat(ENTRY_SEPARATOR).builder()
            .setSkipHeaderRecord(true)
            .setEscape('\\')
            .build();

        // Load the message string as a record, with each comma separated value as a column
        CSVRecord rec = csvFormat.parse(new StringReader(in)).getRecords().get(0);

        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(rec.iterator(), 0), false)
            .map(field -> field.split(KEY_VALUE_SEPARATOR))
            .collect(Collectors.toMap(
                array -> StringUtils.strip(array[0]),   // Key
                array -> StringUtils.strip(array[1])    // Value
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
