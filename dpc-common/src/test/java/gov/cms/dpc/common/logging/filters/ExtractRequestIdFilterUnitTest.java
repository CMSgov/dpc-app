package gov.cms.dpc.common.logging.filters;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import gov.cms.dpc.common.Constants;
import gov.cms.dpc.common.MDCConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.UUID;
import org.mockito.*;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Logger;
import org.slf4j.MDC;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.UriInfo;
import static org.junit.jupiter.api.Assertions.*;

public class ExtractRequestIdFilterUnitTest {
    private ExtractRequestIdFilter filter;

    @Mock
    ContainerRequestContext mockContext;

    @BeforeEach
    public void setUp(){
        MockitoAnnotations.initMocks(this);
        filter = new ExtractRequestIdFilter();
    }

    @Test
    public void testRequestIdIsExtractedOrGenerated() throws IOException {
        String requestId = UUID.randomUUID().toString();
        Mockito.when(mockContext.getHeaderString(ArgumentMatchers.eq(Constants.DPC_REQUEST_ID_HEADER))).thenReturn(requestId);
        UriInfo mockUriInfo = Mockito.mock(UriInfo.class);
        Mockito.when(mockUriInfo.getPath()).thenReturn("v1/Patients");
        Mockito.when(mockContext.getUriInfo()).thenReturn(mockUriInfo);
        Mockito.when(mockContext.getMethod()).thenReturn("GET");

        Logger logger = (Logger) LoggerFactory.getLogger(ExtractRequestIdFilter.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        try {
            listAppender.start();
            logger.addAppender(listAppender);

            //With request-id in header
            assertNull(MDC.get(MDCConstants.DPC_REQUEST_ID));
            filter.filter(mockContext);
            assertEquals("event_type=request-received resource_requested=v1/Patients, method=GET", listAppender.list.get(0).getFormattedMessage());
            assertEquals(requestId, MDC.get(MDCConstants.DPC_REQUEST_ID));

            //Without request-id in header
            MDC.clear();
            Mockito.when(mockContext.getHeaderString(ArgumentMatchers.eq(Constants.DPC_REQUEST_ID_HEADER))).thenReturn(null);
            assertNull(MDC.get(MDCConstants.DPC_REQUEST_ID));
            filter.filter(mockContext);
            assertEquals("event_type=request-received resource_requested=v1/Patients, method=GET", listAppender.list.get(0).getFormattedMessage());
            assertNull(MDC.get(MDCConstants.DPC_REQUEST_ID));
        } finally {
            listAppender.stop();
        }
    }
}