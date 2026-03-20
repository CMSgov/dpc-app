package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.dpc.api.APITestHelpers;
import gov.cms.dpc.api.auth.DPCAuthCredentials;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.auth.staticauth.StaticAuthFilter;
import gov.cms.dpc.api.auth.staticauth.StaticAuthenticator;
import gov.cms.dpc.api.converters.ChecksumConverterProvider;
import gov.cms.dpc.api.converters.HttpRangeHeaderParamConverterProvider;
import gov.cms.dpc.queue.FileManager;
import gov.cms.dpc.common.gzip.GzipUtil;
import gov.cms.dpc.fhir.dropwizard.filters.StreamingContentSizeFilter;
import gov.cms.dpc.queue.IJobQueue;
import gov.cms.dpc.queue.models.JobQueueBatch;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DropwizardExtensionsSupport.class)
@ExtendWith(BufferedLoggerHandler.class)
@SuppressWarnings("InnerClassMayBeStatic")
class DataResourceUnitTest {

    private static final FileManager manager = Mockito.mock(FileManager.class);
    private static final IJobQueue queue = Mockito.mock(IJobQueue.class);
    private static final ResourceExtension RESOURCE = buildDataResource();

    private DataResourceUnitTest() {
        // Not used
    }

    @BeforeEach
    void setup() {
        Mockito.reset(manager);
    }

    static Stream<Arguments> downloadArgs() {
        return Stream.of(
            Arguments.of(false, Collections.EMPTY_MAP),	// uncompressed file, uncompressed download
            Arguments.of(false, Map.of(HttpHeaders.ACCEPT_ENCODING, GzipUtil.GZIP)),	// uncompressed file, compressed download
            Arguments.of(true, Collections.EMPTY_MAP), 	// compressed file, uncompressed download
            Arguments.of(true, Map.of(HttpHeaders.ACCEPT_ENCODING, GzipUtil.GZIP))	// compressed file, compressed download
        );
    }

    @ParameterizedTest
    @MethodSource("downloadArgs")
    void canDownloadFiles(boolean compressFile, Map<String, String> requestHeaders) throws IOException {
        final String testData = "This is a test";

        Mockito.when(manager.getFile(Mockito.any(), Mockito.anyString())).thenAnswer(answer -> {
            final File tempPath = FileUtils.getTempDirectory();
            File file;
            if (compressFile) {
                file = File.createTempFile("test", ".ndjson.gz", tempPath);
                FileUtils.writeByteArrayToFile(file, GzipUtil.compress(testData));
            } else {
                file = File.createTempFile("test", ".ndjson", tempPath);
                FileUtils.write(file, testData, StandardCharsets.UTF_8);
            }
            return new FileManager.FilePointer("", file.length(), UUID.randomUUID(), OffsetDateTime.now(ZoneOffset.UTC), file, compressFile);
        });

        boolean expectGzipCompressed =
            requestHeaders.containsKey(HttpHeaders.ACCEPT_ENCODING) && requestHeaders.get(HttpHeaders.ACCEPT_ENCODING).contains("gzip");

        Invocation.Builder requestBuilder = RESOURCE.target("/v1/Data/test.ndjson")
            .request()
            .header(HttpHeaders.ACCEPT, "application/ndjson");

        for (Map.Entry<String, String> header : requestHeaders.entrySet()) {
            requestBuilder.header(header.getKey(), header.getValue());
        }
        final Response response = requestBuilder.get();

        String responseBody = readResponse(response, expectGzipCompressed);

        assertAll(() -> assertEquals("This is a test", responseBody, "Should have correct string"),
            () -> assertEquals(HttpStatus.OK_200, response.getStatus(), "Should have ok status"),
            () -> assertNotNull(response.getHeaderString(HttpHeaders.ETAG), "Should have eTag"));
    }

    @Test
    void testFileFromExpiredJob() {
        UUID jobId = UUID.randomUUID();

        Mockito.when(manager.getFile(Mockito.any(), Mockito.anyString())).thenAnswer(answer -> {
            final File tempPath = FileUtils.getTempDirectory();
            final File file = File.createTempFile("test", ".ndjson", tempPath);
            FileUtils.write(file, "This is a test", StandardCharsets.UTF_8);
            return new FileManager.FilePointer("", file.length(), jobId, OffsetDateTime.now(ZoneOffset.UTC), file, false);
        });

        UUID aggregatorId = UUID.randomUUID();
        JobQueueBatch jobQueueBatch = new JobQueueBatch(jobId, null, null, null, Collections.emptyList(), null, null, null, null, null, true);
        jobQueueBatch.setRunningStatus(aggregatorId);
        jobQueueBatch.setCompletedStatus(aggregatorId);
        jobQueueBatch.setCompleteTime(OffsetDateTime.now().minusHours(25));
        Mockito.when(queue.getJobBatches(jobId)).thenReturn(List.of(jobQueueBatch));

        final Response response = RESOURCE.target("/v1/Data/test.ndjson")
                .request()
                .get();

        assertEquals(HttpStatus.GONE_410, response.getStatus(), "Should have 410 Gone status");
    }

    @ParameterizedTest
    @MethodSource("downloadArgs")
    void canDownloadFileRanges(boolean compressFile, Map<String, String> requestHeaders) throws IOException {
        final File tempPath = FileUtils.getTempDirectory();
        final int length = 4 * 1024 * 1024;
        final String randomString = buildRandomString();

        File file;
        if (compressFile) {
            file = File.createTempFile("test", ".ndjson.gz", tempPath);
            FileUtils.writeByteArrayToFile(file, GzipUtil.compress(randomString));
        } else {
            file = File.createTempFile("test", ".ndjson", tempPath);
            FileUtils.write(file, randomString, StandardCharsets.UTF_8);
        }

        Mockito.when(manager.getFile(Mockito.any(), Mockito.anyString())).thenReturn(
            new FileManager.FilePointer("", 0, UUID.randomUUID(), OffsetDateTime.now(ZoneOffset.UTC), file, compressFile)
        );

        boolean expectGzipCompressed =
            requestHeaders.containsKey(HttpHeaders.ACCEPT_ENCODING) && requestHeaders.get(HttpHeaders.ACCEPT_ENCODING).contains("gzip");

        // Try to request one byte
        Invocation.Builder requestBuilder = RESOURCE.target("/v1/Data/test.ndjson")
            .request()
            .header(org.apache.hc.core5.http.HttpHeaders.RANGE, "bytes=0-1")
            .header(org.apache.hc.core5.http.HttpHeaders.ACCEPT, "application/ndjson");

        for (Map.Entry<String, String> header : requestHeaders.entrySet()) {
            requestBuilder.header(header.getKey(), header.getValue());
        }
        Response response = requestBuilder.get();
        String responseBody = readResponse(response, expectGzipCompressed);

        assertEquals(HttpStatus.PARTIAL_CONTENT_206, response.getStatus(), "Should have partial content status");
        assertEquals(String.valueOf(randomString.charAt(0)), responseBody, "Should only have a single byte");

        // Request 500 kb, with an offset
        int start = 30;
        int end = 500 * 1024 + start;
        requestBuilder = RESOURCE.target("/v1/Data/test.ndjson")
            .request()
            .header(org.apache.hc.core5.http.HttpHeaders.RANGE, String.format("bytes=%d-%s", start, end))
            .header(org.apache.hc.core5.http.HttpHeaders.ACCEPT, "application/ndjson");

        for (Map.Entry<String, String> header : requestHeaders.entrySet()) {
            requestBuilder.header(header.getKey(), header.getValue());
        }
        response = requestBuilder.get();
        responseBody = readResponse(response, expectGzipCompressed);

        assertEquals(HttpStatus.PARTIAL_CONTENT_206, response.getStatus(), "Should have partial content status");
        assertEquals(randomString.substring(start, end), responseBody, "Response should match");

        // Request the entire file
        requestBuilder = RESOURCE.target("/v1/Data/test.ndjson")
            .request()
            .header(org.apache.hc.core5.http.HttpHeaders.RANGE, String.format("bytes=%d-%s", 0, length))
            .header(org.apache.hc.core5.http.HttpHeaders.ACCEPT, "application/ndjson");

        for (Map.Entry<String, String> header : requestHeaders.entrySet()) {
            requestBuilder.header(header.getKey(), header.getValue());
        }
        response = requestBuilder.get();
        responseBody = readResponse(response, expectGzipCompressed);

        assertEquals(HttpStatus.PARTIAL_CONTENT_206, response.getStatus(), "Should have partial content status");
        assertEquals(randomString, responseBody, "Response should match");

        // Request the entire file, without the ending value, which returns one chunk
        requestBuilder = RESOURCE.target("/v1/Data/test.ndjson")
            .request()
            .header(org.apache.hc.core5.http.HttpHeaders.RANGE, String.format("bytes=%d-", 0))
            .header(org.apache.hc.core5.http.HttpHeaders.ACCEPT, "application/ndjson");

        for (Map.Entry<String, String> header : requestHeaders.entrySet()) {
            requestBuilder.header(header.getKey(), header.getValue());
        }
        response = requestBuilder.get();
        responseBody = readResponse(response, expectGzipCompressed);

        // Result might be chunked, so only check what the serve says it responded with
        String contentRange = response.getHeaderString(org.apache.hc.core5.http.HttpHeaders.CONTENT_RANGE);
        String[] splitContentRange = contentRange.split("[\\s-/]");
        int responseStart = Integer.parseInt(splitContentRange[1]);
        int responseEnd = Integer.parseInt(splitContentRange[2]);

        assertEquals(HttpStatus.PARTIAL_CONTENT_206, response.getStatus(), "Should have partial content status");
        assertEquals(randomString.substring(responseStart, responseEnd), responseBody, "Response should match");

        // Request file with an invalid range
        requestBuilder = RESOURCE.target("/v1/Data/test.ndjson")
            .request()
            .header(org.apache.hc.core5.http.HttpHeaders.RANGE, "bytes=50-0")
            .header(org.apache.hc.core5.http.HttpHeaders.ACCEPT, "application/ndjson");
        for (Map.Entry<String, String> header : requestHeaders.entrySet()) {
            requestBuilder.header(header.getKey(), header.getValue());
        }
        response = requestBuilder.get();

        assertEquals(Response.Status.REQUESTED_RANGE_NOT_SATISFIABLE.getStatusCode(), response.getStatus());
    }

    @Test
    void testNonByteRange() {
        Mockito.when(manager.getFile(Mockito.any(), Mockito.anyString())).thenAnswer(answer -> {
            final File tempPath = FileUtils.getTempDirectory();
            final File file = File.createTempFile("test", ".ndjson", tempPath);
            FileUtils.write(file, "This is a test", StandardCharsets.UTF_8);
            return new FileManager.FilePointer("", file.length(), UUID.randomUUID(), OffsetDateTime.now(ZoneOffset.UTC), file, false);
        });

        final Response response = RESOURCE.target("/v1/Data/test.ndjson")
                .request()
                .header(org.apache.hc.core5.http.HttpHeaders.ACCEPT, "application/ndjson")
                .header(org.apache.hc.core5.http.HttpHeaders.RANGE, "frames=0-1")
                .get();

        assertAll(() -> assertEquals(Response.Status.REQUESTED_RANGE_NOT_SATISFIABLE.getStatusCode(), response.getStatus(), "Should have correct status code"),
                () -> assertEquals("{\"code\":416,\"message\":\"Only `bytes` are acceptable as ranges\"}", response.readEntity(String.class), "Should have correct error message"));
    }

    @Nested
    @DisplayName("Test Cache Header responses")
    class CacheHeaderTests {

        private final OffsetDateTime modifiedDate = LocalDate.of(2017, 3, 11).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();

        @BeforeEach
        void setupETagTests() {

            Mockito.when(manager.getFile(Mockito.any(), Mockito.anyString())).thenAnswer(answer -> {
                final File tempPath = FileUtils.getTempDirectory();
                final File file = File.createTempFile("test", ".ndjson", tempPath);
                FileUtils.write(file, "This is a test", StandardCharsets.UTF_8);
                return new FileManager.FilePointer("This should match", file.length(), UUID.randomUUID(), modifiedDate, file, false);
            });
        }

        @HttpParamTest
        void testMissingETagHeader(String method) {
            final Invocation.Builder builder = RESOURCE.target("/v1/Data/test.ndjson")
                    .request();

            final Response response = createHTTPMethodCall(method, builder);

            assertAll(() -> assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), "Should have downloaded"));
        }

        @HttpParamTest
        void testMismatchingETagHeader(String method) {
            final Invocation.Builder builder = RESOURCE.target("/v1/Data/test.ndjson")
                    .request()
                    .header(org.apache.hc.core5.http.HttpHeaders.ACCEPT, "application/ndjson")
                    .header(HttpHeaders.IF_NONE_MATCH, "Not a real value");

            final Response response = createHTTPMethodCall(method, builder);

            assertAll(() -> assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), "Should have downloaded"));
        }

        @HttpParamTest
        void testCorrectETagHeader(String method) {
            final Invocation.Builder builder = RESOURCE.target("/v1/Data/test.ndjson")
                    .request()
                    .header(HttpHeaders.IF_NONE_MATCH, "This should match");

            final Response response = createHTTPMethodCall(method, builder);

            assertAll(() -> assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), response.getStatus(), "Should have downloaded"));
        }

        @HttpParamTest
        void testWeakETagHeader(String method) {
            final Invocation.Builder builder = RESOURCE.target("/v1/Data/test.ndjson")
                    .request()
                    .header(org.apache.hc.core5.http.HttpHeaders.ACCEPT, "application/ndjson")
                    .header(HttpHeaders.IF_NONE_MATCH, "This should match--gzip");

            final Response response = createHTTPMethodCall(method, builder);

            assertAll(() -> assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), response.getStatus(), "Should have downloaded"));
        }

        @HttpParamTest
        void testModifiedTimestamp(String method) {

            final Invocation.Builder builder = RESOURCE.target("/v1/Data/test.ndjson")
                    .request()
                    .header(org.apache.hc.core5.http.HttpHeaders.ACCEPT, "application/ndjson")
                    .header(HttpHeaders.IF_MODIFIED_SINCE, modifiedDate.toInstant().toEpochMilli());

            final Response response = createHTTPMethodCall(method, builder);

            assertAll(() -> assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), response.getStatus(), "Should have downloaded"));
        }

        @HttpParamTest
        void testMalformedModifiedTimestamp(String method) {
            final Invocation.Builder builder = RESOURCE.target("/v1/Data/test.ndjson")
                    .request()
                    .header(HttpHeaders.IF_MODIFIED_SINCE, "Not a real value");

            final Response response = createHTTPMethodCall(method, builder);

            assertAll(() -> assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), "Should have downloaded"));
        }

        private Response createHTTPMethodCall(String method, Invocation.Builder builder) {
            if (method.equals("HEAD")) {
                return builder.head();
            } else if (method.equals("GET")) {
                return builder.get();
            }
            throw new IllegalStateException(String.format("HTTP Method %s is not supported", method));
        }
    }

    private static ResourceExtension buildDataResource() {

        final DataResource dataResource = new DataResource(manager, queue);
        final FhirContext ctx = FhirContext.forDstu3();
        final AuthFilter<DPCAuthCredentials, OrganizationPrincipal> staticFilter = new StaticAuthFilter(new StaticAuthenticator());

        return APITestHelpers.buildResourceExtension(ctx, Collections.singletonList(dataResource),
                List.of(staticFilter,
                        new AuthValueFactoryProvider.Binder<>(OrganizationPrincipal.class),
                        new HttpRangeHeaderParamConverterProvider(),
                        new ChecksumConverterProvider(),
                        new StreamingContentSizeFilter()), false);
    }

    private static String buildRandomString() throws IOException {
        Random rnd = new Random();
        try (StringWriter writer = new StringWriter()) {
            for (int i = 0; i < (long) 4194304; i++) {
                char c = (char) (rnd.nextInt(26) + 'a');
                writer.write(c);
            }
            writer.flush();
            return writer.toString();
        }
    }

    private String readCompressedResponse(Response response) throws IOException {
        final InputStream output = response.readEntity(InputStream.class);
        return GzipUtil.decompress(output.readAllBytes());
    }

    private String readResponse(Response response, boolean isCompressed) throws IOException {
        if (isCompressed) {
            return readCompressedResponse(response);
        } else {
            final InputStream inputStream = response.readEntity(InputStream.class);
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @ParameterizedTest(name = "{arguments} request")
    @ValueSource(strings = {"HEAD", "GET"})
    @interface HttpParamTest {
    }
}
