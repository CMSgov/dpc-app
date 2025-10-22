package gov.cms.dpc.fhir.dropwizard.filters;

import gov.cms.dpc.testing.BufferedLoggerHandler;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.hc.core5.http.HttpHeaders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import static gov.cms.dpc.fhir.dropwizard.filters.StreamingContentSizeFilter.X_CONTENT_LENGTH;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(BufferedLoggerHandler.class)
public class StreamingContentSizeFilterTest {

    private static final StreamingContentSizeFilter filter = new StreamingContentSizeFilter();
    private static final ContainerResponseContext context = Mockito.mock(ContainerResponseContext.class);

    @AfterEach
    void cleanup() {
        Mockito.reset(context);
    }


    @Test
    void testNoContentLength() {
        filter.filter(null, context);
        Mockito.verify(context, Mockito.never()).getHeaders();
    }

    @Test
    void testHeaderModification() {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add(HttpHeaders.TRANSFER_ENCODING, "nothing");
        headers.add(X_CONTENT_LENGTH, "nothing");
        Mockito.when(context.getHeaderString(X_CONTENT_LENGTH)).thenReturn("this is the length");
        Mockito.when(context.getHeaders()).thenAnswer(answer -> headers);
        filter.filter(null, context);
        Mockito.verify(context, Mockito.times(3)).getHeaders();
        assertAll(() -> assertNull(headers.get(HttpHeaders.TRANSFER_ENCODING), "Should not have transfer encoding"),
                () -> assertNull(headers.get(X_CONTENT_LENGTH), "Should not have custom content length"),
                () -> assertEquals("this is the length", headers.getFirst(HttpHeaders.CONTENT_LENGTH), "Should have new content length"));
    }
}
