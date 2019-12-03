package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.dpc.api.APITestHelpers;
import gov.cms.dpc.api.auth.DPCAuthCredentials;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.auth.staticauth.StaticAuthFilter;
import gov.cms.dpc.api.auth.staticauth.StaticAuthenticator;
import gov.cms.dpc.api.converters.ChecksumConverterProvider;
import gov.cms.dpc.api.converters.HttpRangeHeaderParamConverterProvider;
import gov.cms.dpc.api.core.FileManager;
import gov.cms.dpc.fhir.dropwizard.filters.StreamingContentSizeFilter;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DropwizardExtensionsSupport.class)
@ExtendWith(BufferedLoggerHandler.class)
@SuppressWarnings("InnerClassMayBeStatic")
class DataResourceTest {

    private static FileManager manager = Mockito.mock(FileManager.class);
    private static final ResourceExtension RESOURCE = buildDataResource();

    private DataResourceTest() {
        // Not used
    }

    @BeforeEach
    void setup() {
        Mockito.reset(manager);
    }

    @Test
    void streamingTest() throws IOException {

        Mockito.when(manager.getFile(Mockito.any(), Mockito.anyString())).thenAnswer(answer -> {
            final File tempPath = FileUtils.getTempDirectory();
            final File file = File.createTempFile("test", ".ndjson", tempPath);
            FileUtils.write(file, "This is a test", StandardCharsets.UTF_8);
            return new FileManager.FilePointer("", file.length(), UUID.randomUUID(), OffsetDateTime.now(ZoneOffset.UTC), file);
        });

        final Response response = RESOURCE.target("/v1/Data/test.ndjson")
                .request()
                .get();

        final InputStream output = response.readEntity(InputStream.class);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        IOUtils.copy(output, bos);
        assertAll(() -> assertEquals("This is a test", bos.toString(StandardCharsets.UTF_8), "Should have correct string"),
                () -> assertEquals(HttpStatus.OK_200, response.getStatus(), "Should have ok status"),
                () -> assertNotNull(response.getHeaderString(HttpHeaders.ETAG), "Should have eTag"));
    }

    @Test
    void testRangeRequest() throws IOException {
        final File tempPath = FileUtils.getTempDirectory();
        final File file = File.createTempFile("test", ".ndjson", tempPath);
        final int length = 4 * 1024 * 1024;
        final String randomString = buildRandomString(length);
        FileUtils.write(file, randomString, StandardCharsets.UTF_8);

        Mockito.when(manager.getFile(Mockito.any(), Mockito.anyString())).thenReturn(new FileManager.FilePointer("", 0, UUID.randomUUID(), OffsetDateTime.now(ZoneOffset.UTC), file));

        // Try to request one byte
        Response response = RESOURCE.target("/v1/Data/test.ndjson")
                .request()
                .header(org.apache.http.HttpHeaders.RANGE, "bytes=0-1")
                .get();

        InputStream is = response.readEntity(InputStream.class);
        StringWriter stringWriter = new StringWriter();
        IOUtils.copy(is, stringWriter, StandardCharsets.UTF_8);

        final String ch1 = response.getHeaderString(org.apache.http.HttpHeaders.CONTENT_LENGTH);
        final String ws1 = stringWriter.toString();
        assertAll(() -> assertNotNull(ch1, "Should have header"),
                () -> assertEquals(1, Integer.parseInt(ch1), "Should only have a single byte"),
                () -> assertEquals(String.valueOf(randomString.charAt(0)), ws1, "Should only have a single byte"));
        stringWriter.getBuffer().setLength(0);

        // Request 500 kb, with an offset
        int start = 30;
        int end = 500 * 1024 + start;
        response = RESOURCE.target("/v1/Data/test.ndjson")
                .request()
                .header(org.apache.http.HttpHeaders.RANGE, String.format("bytes=%d-%s", start, end))
                .get();

        is = response.readEntity(InputStream.class);
        stringWriter = new StringWriter();
        IOUtils.copy(is, stringWriter, StandardCharsets.UTF_8);

        final String ch2 = response.getHeaderString(org.apache.http.HttpHeaders.CONTENT_LENGTH);
        final String ws2 = stringWriter.toString();
        assertAll(() -> assertNotNull(ch1, "Should have header"),
                () -> assertEquals(500 * 1024, Integer.parseInt(ch2), "Should have 500 kb"),
                () -> assertEquals(randomString.substring(start, end), ws2, "Strings should match"));
        stringWriter.getBuffer().setLength(0);

        // Request the entire file
        response = RESOURCE.target("/v1/Data/test.ndjson")
                .request()
                .header(org.apache.http.HttpHeaders.RANGE, String.format("bytes=0-%s", length))
                .get();

        is = response.readEntity(InputStream.class);
        stringWriter = new StringWriter();
        IOUtils.copy(is, stringWriter, StandardCharsets.UTF_8);

        final String ch3 = response.getHeaderString(org.apache.http.HttpHeaders.CONTENT_LENGTH);
        final String ws3 = stringWriter.toString();
        assertAll(() -> assertNotNull(ch1, "Should have header"),
                () -> assertEquals(length, Integer.parseInt(ch3), "Should have correct file length"),
                () -> assertEquals(randomString, ws3, "Strings should match"));
        stringWriter.getBuffer().setLength(0);

        // Request the entire file, without the ending value, which returns one chunk
        response = RESOURCE.target("/v1/Data/test.ndjson")
                .request()
                .header(org.apache.http.HttpHeaders.RANGE, "bytes=0-")
                .get();

        is = response.readEntity(InputStream.class);
        stringWriter = new StringWriter();
        IOUtils.copy(is, stringWriter, StandardCharsets.UTF_8);

        final String ch4 = response.getHeaderString(org.apache.http.HttpHeaders.CONTENT_LENGTH);
        final String ws4 = stringWriter.toString();
        assertAll(() -> assertNotNull(ch1, "Should have header"),
                () -> assertEquals(1024 * 1024, Integer.parseInt(ch4), "Should have 1 MB chunk"),
                () -> assertEquals(randomString.substring(0, 1024 * 1024), ws4, "Should match the first 1MB of the file"));

        // Request file with an invalid range
        response = RESOURCE.target("/v1/Data/test.ndjson")
                .request()
                .header(org.apache.http.HttpHeaders.RANGE, "bytes=50-0")
                .get();

        assertEquals(Response.Status.REQUESTED_RANGE_NOT_SATISFIABLE.getStatusCode(), response.getStatus());
        assertEquals("{\"code\":416,\"message\":\"Range end cannot be before begin\"}", response.readEntity(String.class), "Should have correct status code");
    }


    @Nested
    @DisplayName("Test ETag responses")
    class ETagTests {

        private final OffsetDateTime modifiedDate = LocalDate.of(2017, 3, 11).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();

        @BeforeEach
        void setupETagTests() {

            Mockito.when(manager.getFile(Mockito.any(), Mockito.anyString())).thenAnswer(answer -> {
                final File tempPath = FileUtils.getTempDirectory();
                final File file = File.createTempFile("test", ".ndjson", tempPath);
                FileUtils.write(file, "This is a test", StandardCharsets.UTF_8);
                return new FileManager.FilePointer("This should match", file.length(), UUID.randomUUID(), modifiedDate, file);
            });
        }

        @Test
        void testMissingETagHeader() {
            final Response response = RESOURCE.target("/v1/Data/test.ndjson")
                    .request()
                    .get();

            assertAll(() -> assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), "Should have downloaded"));
        }

        @Test
        void testMismatchingETagHeader() {
            final Response response = RESOURCE.target("/v1/Data/test.ndjson")
                    .request()
                    .header(HttpHeaders.IF_NONE_MATCH, "Not a real value")
                    .get();

            assertAll(() -> assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), "Should have downloaded"));
        }

        @Test
        void testCorrectETagHeader() {
            final Response response = RESOURCE.target("/v1/Data/test.ndjson")
                    .request()
                    .header(HttpHeaders.IF_NONE_MATCH, "This should match")
                    .get();

            assertAll(() -> assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), response.getStatus(), "Should have downloaded"));
        }

        @Test
        void testWeakETagHeader() {
            final Response response = RESOURCE.target("/v1/Data/test.ndjson")
                    .request()
                    .header(HttpHeaders.IF_NONE_MATCH, "This should match--gzip")
                    .get();

            assertAll(() -> assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), response.getStatus(), "Should have downloaded"));
        }

        @Test
        void testModifiedTimestamp() {

            final Response response = RESOURCE.target("/v1/Data/test.ndjson")
                    .request()
                    .header(HttpHeaders.IF_MODIFIED_SINCE, modifiedDate.toInstant().toEpochMilli())
                    .get();

            assertAll(() -> assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), response.getStatus(), "Should have downloaded"));
        }

        @Test
        void testMalformedModifiedTimestamp() {
            final Response response = RESOURCE.target("/v1/Data/test.ndjson")
                    .request()
                    .header(HttpHeaders.IF_MODIFIED_SINCE, "Not a real value")
                    .get();

            assertAll(() -> assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), "Should have downloaded"));
        }
    }

    private static ResourceExtension buildDataResource() {

        final DataResource dataResource = new DataResource(manager);
        final FhirContext ctx = FhirContext.forDstu3();
        final AuthFilter<DPCAuthCredentials, OrganizationPrincipal> staticFilter = new StaticAuthFilter(new StaticAuthenticator());

        return APITestHelpers.buildResourceExtension(ctx, Collections.singletonList(dataResource),
                List.of(staticFilter,
                        new AuthValueFactoryProvider.Binder<>(OrganizationPrincipal.class),
                        new HttpRangeHeaderParamConverterProvider(),
                        new ChecksumConverterProvider()), false);
    }

    private static String buildRandomString(long length) throws IOException {
        Random rnd = new Random();
        try (StringWriter writer = new StringWriter()) {
            for (int i = 0; i < length; i++) {
                char c = (char) (rnd.nextInt(26) + 'a');
                writer.write(c);
            }
            writer.flush();
            return writer.toString();
        }
    }
}
