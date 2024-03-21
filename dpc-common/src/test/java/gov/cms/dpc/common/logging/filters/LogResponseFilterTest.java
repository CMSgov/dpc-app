package gov.cms.dpc.common.logging.filters;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import gov.cms.dpc.common.Constants;
import gov.cms.dpc.common.MDCConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

public class LogResponseFilterTest {

    private LogResponseFilter filter;

    @Mock
    ContainerResponseContext mockResponse;

    @Mock
    ContainerRequestContext mockRequest;


    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        filter = new LogResponseFilter();
    }

    @Test
    void testFilter() {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        Mockito.when(mockResponse.getHeaders()).thenReturn(headers);
        UriInfo mockUriInfo = Mockito.mock(UriInfo.class);
        Mockito.when(mockUriInfo.getPath()).thenReturn("v1/Patients");
        Mockito.when(mockRequest.getUriInfo()).thenReturn(mockUriInfo);
        Mockito.when(mockRequest.getMethod()).thenReturn("GET");
        MediaType mockMediaType = Mockito.mock(MediaType.class);
        Mockito.when(mockMediaType.toString()).thenReturn("application/json");
        Mockito.when(mockRequest.getMediaType()).thenReturn(mockMediaType);


        Logger logger = (Logger) LoggerFactory.getLogger(LogResponseFilter.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        try {
            listAppender.start();
            logger.addAppender(listAppender);

            //With request-id mdc
            String requestId = UUID.randomUUID().toString();
            MDC.clear();
            MDC.put(MDCConstants.DPC_REQUEST_ID, requestId);
            Mockito.when(mockResponse.getStatus()).thenReturn(200);
            filter.filter(mockRequest,mockResponse);
            assertEquals("resource_requested=v1/Patients, media_type=application/json, method=GET, status=200", listAppender.list.get(0).getFormattedMessage());
            assertEquals(1,headers.get(Constants.DPC_REQUEST_ID_HEADER).size());
            assertEquals(requestId,headers.get(Constants.DPC_REQUEST_ID_HEADER).get(0));

            //Without request-id in mdc
            MDC.clear();
            Mockito.when(mockResponse.getStatus()).thenReturn(202);
            filter.filter(mockRequest, mockResponse);
            assertEquals("resource_requested=v1/Patients, media_type=application/json, method=GET, status=202", listAppender.list.get(1).getFormattedMessage());
            assertFalse(headers.containsKey(MDCConstants.DPC_REQUEST_ID));

        } finally {
            listAppender.stop();
        }
    }
}