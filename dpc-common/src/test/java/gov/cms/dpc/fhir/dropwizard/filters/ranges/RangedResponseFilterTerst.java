package gov.cms.dpc.fhir.dropwizard.filters.ranges;

import gov.cms.dpc.fhir.dropwizard.filters.ranges.RangeResponseFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.io.ByteArrayOutputStream;

/**
 * Test for {@link RangeResponseFilter}.
 */
class RangeResponseFilterTest {

    private ContainerRequestContext requestContext;

    private ContainerResponseContext responseContext;

    private MultivaluedMap<String, String> requestHeaders;

    private MultivaluedMap<String, String> responseHeaders;

    @BeforeEach
    private void init() {
        this.requestContext = Mockito.mock(ContainerRequestContext.class);
        this.responseContext = Mockito.mock(ContainerResponseContext.class);
        this.requestHeaders = new MultivaluedHashMap<>();
        this.responseHeaders = new MultivaluedHashMap<>();
        Mockito.doReturn(this.requestHeaders).when(this.requestContext).getHeaders();
        Mockito.doReturn(this.responseHeaders).when(this.responseContext).getHeaders();
        MediaType mediaType = Mockito.mock(MediaType.class);
        Mockito.doReturn(mediaType).when(this.responseContext).getMediaType();
        //noinspection ResultOfMethodCallIgnored
        Mockito.doReturn("text/plain").when(mediaType).toString();
    }

    /**
     * Tests the filter with If-Range (E-Tag) header.
     */
    @Test
    void filterIfRangeEtagTest() {
        this.requestHeaders.putSingle("Range", "bytes=6-10");
        this.requestHeaders.putSingle("If-Range", "qwerty");
        Mockito.doReturn("bytes=6-10").when(this.requestContext).getHeaderString("Range");
        Mockito.doReturn("qwerty").when(this.requestContext).getHeaderString("If-Range");

        this.responseHeaders.putSingle(HttpHeaders.ETAG, "qwerty");
        Mockito.doReturn("qwerty").when(this.responseContext).getHeaderString(HttpHeaders.ETAG);
        Mockito.doReturn(new ByteArrayOutputStream()).when(this.responseContext).getEntityStream();

        RangeResponseFilter filter = new RangeResponseFilter();
        filter.filter(this.requestContext, this.responseContext);
    }

    /**
     * Tests the filter with If-Range (Last-Modified) header.
     */
    @Test
    void filterIfRangeLastModifiedTest() {
        this.requestHeaders.putSingle("Range", "bytes=6-10");
        this.requestHeaders.putSingle("If-Range", "Wed, 21 Oct 2015 07:28:00 GMT");
        Mockito.doReturn(this.requestHeaders).when(this.requestContext).getHeaders();
        Mockito.doReturn("bytes=6-10").when(this.requestContext).getHeaderString("Range");
        Mockito.doReturn("Wed, 21 Oct 2015 07:28:00 GMT").when(this.requestContext).getHeaderString("If-Range");

        this.responseHeaders.putSingle(HttpHeaders.LAST_MODIFIED, "Wed, 21 Oct 2015 07:28:00 GMT");
        Mockito.doReturn("Wed, 21 Oct 2015 07:28:00 GMT").when(this.responseContext)
                .getHeaderString(HttpHeaders.LAST_MODIFIED);
        Mockito.doReturn(new ByteArrayOutputStream()).when(this.responseContext).getEntityStream();

        RangeResponseFilter filter = new RangeResponseFilter();
        filter.filter(this.requestContext, this.responseContext);
    }

    /**
     * Tests the filter without If-Range header.
     */
    @Test
    void filterWithoutIfRangeTest() {
        this.requestHeaders.putSingle("Range", "bytes=6-10");
        Mockito.doReturn(this.requestHeaders).when(this.requestContext).getHeaders();
        Mockito.doReturn("bytes=6-10").when(this.requestContext).getHeaderString("Range");

        Mockito.doReturn(this.responseHeaders).when(this.responseContext).getHeaders();
        Mockito.doReturn(new ByteArrayOutputStream()).when(this.responseContext).getEntityStream();

        RangeResponseFilter filter = new RangeResponseFilter();
        filter.filter(this.requestContext, this.responseContext);
    }

}
