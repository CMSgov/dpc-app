package gov.cms.dpc.fhir.dropwizard.filters;

import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import static gov.cms.dpc.fhir.dropwizard.filters.StreamingContentSizeFilter.X_CONTENT_LENGTH;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;

@ExtendWith(BufferedLoggerHandler.class)
@DisplayName("Streaming content filtering")
public class StreamingContentSizeFilterTest {

    private static final StreamingContentSizeFilter filter = new StreamingContentSizeFilter();
    private static final ContainerResponseContext context = Mockito.mock(ContainerResponseContext.class);

    @AfterEach
    void cleanup() {
        Mockito.reset(context);
    }


    @Test
    @DisplayName("Filter null content 🥳")
    void testNoContentLength() {
        filter.filter(null, context);
        Mockito.verify(context, Mockito.never()).getHeaders();
    }

    @Test
    @DisplayName("Modify streaming content headers 🥳")
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
