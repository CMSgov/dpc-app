package gov.cms.dpc.common.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.pattern.ThrowableHandlingConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggerContextVO;
import ch.qos.logback.core.read.ListAppender;
import com.google.common.collect.Maps;
import io.dropwizard.logging.json.EventAttribute;
import io.dropwizard.logging.json.layout.JsonFormatter;
import io.dropwizard.logging.json.layout.TimestampFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import org.hibernate.exception.ConstraintViolationException;
import java.sql.SQLException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


public class DPCJsonLayoutUnitTest {

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

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        dpcJsonLayout = new DPCJsonLayout(jsonFormatter,
                timestampFormatter,
                throwableHandlingConverter,
                Set.of(EventAttribute.MESSAGE, EventAttribute.EXCEPTION),
                new HashMap<>(),
                new HashMap<>(),
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
        assertEquals(message, map.get("message"));

        message = "key1=value2, key2=value2, key3NoVal";
        when(loggingEvent.getFormattedMessage()).thenReturn(message);
        map = dpcJsonLayout.toJsonMap(loggingEvent);
        assertEquals(message, map.get("message"));
    }

    @Test
    public void changeWhenMessageIsParsableAsMap() {
        String message = "key1=value1, key2=value2";

        when(loggingEvent.getFormattedMessage()).thenReturn(message);
        Map<String, Object> map = dpcJsonLayout.toJsonMap(loggingEvent);
        assertFalse(map.containsKey("message"));
        assertEquals("value1", map.get("key1"));
        assertEquals("value2", map.get("key2"));
    }

    @Test
    public void testLookBackLogJsonParsing() {
        String message = "billingPeriodDate=Thu Jul 01 00:00:00 UTC 1999, lookBackDate=Thu Aug 27 00:43:30 UTC 2020, monthsDifference=253, eobProvider=null, eobCareTeamProviders=999999999999;9999999999, jobProvider=1232125215, eobOrganization=9999999999, jobOrganization=5808156785, withinLimit=false, eobProviderMatch=false, eobOrganizationMatch=false";
        when(loggingEvent.getFormattedMessage()).thenReturn(message);
        Map<String, Object> map = dpcJsonLayout.toJsonMap(loggingEvent);
        assertFalse(map.containsKey("message"));
        assertEquals("999999999999;9999999999", map.get("eobCareTeamProviders"));
    }

    @Test
    public void testMBIMasking() {
        Map<String,String> inputOutputMap = Maps.newHashMap();
        inputOutputMap.put("1SQ3F00AA00", "***MBI?***");
        inputOutputMap.put("mbi1SQ3F00AA00", "mbi***MBI?***");
        inputOutputMap.put(" 1SQ3F00AA00", " ***MBI?***");
        inputOutputMap.put("1SQ3F00AA00 ", "***MBI?*** ");
        inputOutputMap.put("rAnDoM1SQ3F00AA00", "rAnDoM***MBI?***");
        inputOutputMap.put("1SQ3F00AA00rANDom1", "***MBI?***rANDom1");
        inputOutputMap.put("11SQ3F00AA00", "1***MBI?***");
        inputOutputMap.put("random text 1SQ3F00AA00", "random text ***MBI?***");
        inputOutputMap.put("21SQ3F00AA002", "2***MBI?***2");
        inputOutputMap.put("", "");
        inputOutputMap.put(" ", " ");

        inputOutputMap.entrySet().stream().forEach(entry -> {
            final String unMaskedMessage = entry.getKey();
            final String expectedMaskedMessage = entry.getValue();
            when(loggingEvent.getFormattedMessage()).thenReturn(unMaskedMessage);
            Map<String, Object> map = dpcJsonLayout.toJsonMap(loggingEvent);
            assertEquals(expectedMaskedMessage, map.get("message"));
        });
    }

    @Test
    public void testMBIMaskingWhenMessageIsParsableAsMap() {
        final String message = "key1=value1, key2=1SQ3F00AA00, key3=value3";

        when(loggingEvent.getFormattedMessage()).thenReturn(message);
        Map<String, Object> map = dpcJsonLayout.toJsonMap(loggingEvent);
        assertFalse(map.containsKey("message"));
        assertEquals("value1", map.get("key1"));
        assertEquals("***MBI?***", map.get("key2"));
        assertEquals("value3", map.get("key3"));
    }

    @Test
    public void testPostgresMasking() {
        String badLogMessage = "[ERROR] org.postgresql.util.PSQLException: ERROR: duplicate key value violates unique constraint \"organization_idx\"\n" +
                "  Detail: Key (id_system, id_value)=(1, 1111111112) already exists.\n" +
                "\tat org.postgresql.core.v3.QueryExecutorImpl.receiveErrorResponse(QueryExecutorImpl.java:2725)\n" +
                "\tat org.postgresql.core.v3.QueryExecutorImpl.processResults(QueryExecutorImpl.java:2412)\n";
        String expectedLogMessage = "[ERROR] org.postgresql.util.PSQLException: ERROR: duplicate key value violates unique constraint \"organization_idx\"\n" +
                "  **********\n" +
                "\tat org.postgresql.core.v3.QueryExecutorImpl.receiveErrorResponse(QueryExecutorImpl.java:2725)\n" +
                "\tat org.postgresql.core.v3.QueryExecutorImpl.processResults(QueryExecutorImpl.java:2412)\n";
        when(loggingEvent.getFormattedMessage()).thenReturn(badLogMessage);
        Map<String, Object> map = dpcJsonLayout.toJsonMap(loggingEvent);
        assertEquals(expectedLogMessage, map.get("message"));
    }

    @Test
    public void testBatchMessageMasking() {
        String reallyLongLogMessage = "Wrapped by: java.sql.BatchUpdateException: Batch entry 0 /* insert gov.cms.dpc.common.entities.OrganizationEntity */ insert into organizations (city, country, district, line1, line2, postal_code, state, address_type, address_use, id_system, id_value, organization_name, id) values (('Akron'), ('US'), (NULL), ('111 Main ST'), ('STE 5'), ('22222'), ('OH'), ('2'::int4), ('1'::int4), ('1'::int4), ('1111111112'), ('Org'), ('d2fcd068-a818-4874-9fc2-fd9633b073a2'::uuid)) was aborted: ERROR: duplicate key value violates unique constraint \\\"organization_idx\\\"\\n  Detail: some bad info here";
        String expectedLogMessage = "Wrapped by: java.sql.BatchUpdateException: **********: ERROR: duplicate key value violates unique constraint \\\"organization_idx\\\"\\n  **********";
        when(loggingEvent.getFormattedMessage()).thenReturn(reallyLongLogMessage);
        Map<String, Object> map = dpcJsonLayout.toJsonMap(loggingEvent);
        assertEquals(expectedLogMessage, map.get("message"));
    }

    @Test
    public void testPostgresMaskingOnException() {
        String badLogOnException = "2024-11-01 12:23:25 {\"timestamp\":\"2024-11-01T19:23:25.359+0000\"," +
                "\"level\":\"ERROR\",\"thread\":\"pool-3-thread-6\"," +
                "\"logger\":\"io.dropwizard.jersey.errors.LoggingExceptionMapper\"," +
                "\"message\":\"Error handling a request: 33abf771288c609f\"," +
                "\"exception\":\"org.postgresql.util.PSQLException: ERROR: duplicate key value violates unique constraint \\\"organization_idx\\\"\\n  Detail: Key (id_system, id_value)=(1, 1111111112) already exists.\\n\\tat org.postgresql.core.v3.QueryExecutorImpl.receiveErrorResponse(QueryExecutorImpl.java:2725)\\n\\tat ";
        String expectedLogMessage = "2024-11-01 12:23:25 {\"timestamp\":\"2024-11-01T19:23:25.359+0000\"," +
                "\"level\":\"ERROR\",\"thread\":\"pool-3-thread-6\"," +
                "\"logger\":\"io.dropwizard.jersey.errors.LoggingExceptionMapper\"," +
                "\"message\":\"Error handling a request: 33abf771288c609f\"," +
                "\"exception\":\"org.postgresql.util.PSQLException: ERROR: duplicate key value violates unique constraint \\\"organization_idx\\\"\\n  **********";

        when(throwableHandlingConverter.convert(any())).thenReturn(badLogOnException);
        Logger logger = (Logger) LoggerFactory.getLogger("justtesting");
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        // log exception with nested database info
        SQLException sqlException = new SQLException("/* insert gov.cms.dpc.common.entities.OrganizationEntity */ insert into organizations (city, country, district, line1, line2, postal_code, state, address_type, address_use, id_system, id_value, organization_name, id) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        ConstraintViolationException exception = new ConstraintViolationException("could not execute batch", sqlException, "organization_idx");
        logger.error("Error handling a request: 33211570adcaad95", exception);

        // Retrieve the LoggingEvent object
        ILoggingEvent loggingEvent = listAppender.list.stream()
                .filter(event -> event.getFormattedMessage().contains("Error handling a request: 33211570adcaad95"))
                .findFirst()
                .orElse(null);

        Map<String, Object> map = dpcJsonLayout.toJsonMap(loggingEvent);
        assertEquals(expectedLogMessage, map.get("exception"));
    }

    @Test
    public void testQuotedValues() {
        String message = "key1=value1, key2=\"value2a,value2b\"";

        when(loggingEvent.getFormattedMessage()).thenReturn(message);
        Map<String, Object> map = dpcJsonLayout.toJsonMap(loggingEvent);
        assertFalse(map.containsKey("message"));
        assertEquals("value1", map.get("key1"));
        assertEquals("value2a,value2b", map.get("key2"));
    }
}
