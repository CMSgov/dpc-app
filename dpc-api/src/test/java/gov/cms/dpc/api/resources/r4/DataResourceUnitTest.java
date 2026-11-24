package gov.cms.dpc.api.resources.r4;

import ca.uhn.fhir.context.FhirContext;
import com.github.benmanes.caffeine.cache.Caffeine;
import gov.cms.dpc.api.r4.APITestHelpers;
import gov.cms.dpc.api.auth.staticauth.StaticAuthFilter;
import gov.cms.dpc.api.auth.staticauth.StaticAuthenticator;
import gov.cms.dpc.api.converters.ChecksumConverterProvider;
import gov.cms.dpc.api.converters.HttpRangeHeaderParamConverterProvider;
import gov.cms.dpc.attribution.client.AttributionClient;
import gov.cms.dpc.queue.FileManager;
import gov.cms.dpc.common.utils.GzipUtil;
import gov.cms.dpc.common.utils.NPIUtil;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.dropwizard.filters.StreamingContentSizeFilter;
import gov.cms.dpc.queue.IJobQueue;
import gov.cms.dpc.queue.models.JobQueueBatch;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.r4.model.Organization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Stream;

import static org.apache.hc.core5.http.HttpHeaders.CONTENT_RANGE;
import static org.apache.hc.core5.http.HttpHeaders.RANGE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(DropwizardExtensionsSupport.class)
@ExtendWith(BufferedLoggerHandler.class)
@SuppressWarnings("InnerClassMayBeStatic")
class DataResourceUnitTest {

    private static final FileManager manager = mock(FileManager.class);
    private static final AttributionClient client = mock(AttributionClient.class);
    private static final IJobQueue queue = mock(IJobQueue.class);
    private static final ResourceExtension RESOURCE = buildDataResource();

    private DataResourceUnitTest() {
        // Not used
    }

    @BeforeEach
    void setup() {
        reset(manager, client);

        final Organization org = new Organization();
        final String orgNPI = NPIUtil.generateNPI();
        org.addIdentifier().setSystem(DPCIdentifierSystem.NPPES.getSystem()).setValue(orgNPI);
        when(client.getOrganization(Mockito.any(UUID.class))).thenReturn(org);
    }

    static Stream<Arguments> downloadArgs() {
        return Stream.of(
            Arguments.of(false, Collections.EMPTY_MAP), // gzip in .ndjson, uncompressed download
            Arguments.of(false, Map.of(HttpHeaders.ACCEPT_ENCODING, GzipUtil.GZIP)), // gzip in .ndjson, compressed download
            Arguments.of(true, Collections.EMPTY_MAP), // legacy .ndjson.gz, uncompressed download
            Arguments.of(true, Map.of(HttpHeaders.ACCEPT_ENCODING, GzipUtil.GZIP)) // legacy .ndjson.gz, compressed download
        );
    }

    @ParameterizedTest
    @MethodSource("downloadArgs")
    // All files are compressed, but legacy files are .ndjson.gz and new files are just .ndjson
    void canDownloadFiles(boolean legacyExtension, Map<String, String> requestHeaders) throws IOException {
        final String testData = "This is a test";

        when(manager.getV3File(Mockito.any(), Mockito.anyString())).thenAnswer(answer -> {
            final File tempPath = FileUtils.getTempDirectory();
            File file = File.createTempFile("test", legacyExtension ? ".ndjson.gz" : ".ndjson", tempPath);
            FileUtils.writeByteArrayToFile(file, GzipUtil.compress(testData));
            return new FileManager.FilePointer("", file.length(), UUID.randomUUID(), OffsetDateTime.now(ZoneOffset.UTC), file, true);
        });

        boolean expectGzipCompressed =
            requestHeaders.containsKey(HttpHeaders.ACCEPT_ENCODING) && requestHeaders.get(HttpHeaders.ACCEPT_ENCODING).contains("gzip");

        Invocation.Builder requestBuilder = RESOURCE.target("/v3/Data/test.ndjson")
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

        when(manager.getV3File(Mockito.any(), Mockito.anyString())).thenAnswer(answer -> {
            final File tempPath = FileUtils.getTempDirectory();
            final File file = File.createTempFile("test", ".ndjson", tempPath);
            FileUtils.writeByteArrayToFile(file, GzipUtil.compress("This is a test"));
            return new FileManager.FilePointer("", file.length(), jobId, OffsetDateTime.now(ZoneOffset.UTC), file, true);
        });

        UUID aggregatorId = UUID.randomUUID();
        JobQueueBatch jobQueueBatch = new JobQueueBatch.Builder()
                .jobID(jobId)
                .patients(Collections.emptyList())
                .isBulk(true)
                .build();
        jobQueueBatch.setRunningStatus(aggregatorId);
        jobQueueBatch.setCompletedStatus(aggregatorId);
        jobQueueBatch.setCompleteTime(OffsetDateTime.now().minusHours(25));
        when(queue.getJobBatches(jobId)).thenReturn(List.of(jobQueueBatch));

        final Response response = RESOURCE.target("/v3/Data/test.ndjson")
                .request()
                .get();

        assertEquals(HttpStatus.GONE_410, response.getStatus(), "Should have 410 Gone status");
    }

    @ParameterizedTest
    @MethodSource("downloadArgs")
    void canDownloadFileRanges(boolean legacyExtension, Map<String, String> requestHeaders) throws IOException {
        final File tempPath = FileUtils.getTempDirectory();
        final int length = 4 * 1024 * 1024;
        final String randomString = buildRandomString();

        File file = File.createTempFile("test", legacyExtension ? ".ndjson.gz" : ".ndjson", tempPath);
        FileUtils.writeByteArrayToFile(file, GzipUtil.compress(randomString));

        when(manager.getV3File(Mockito.any(), Mockito.anyString())).thenReturn(
            new FileManager.FilePointer("", file.length(), UUID.randomUUID(), OffsetDateTime.now(ZoneOffset.UTC), file, true)
        );

        Response head = RESOURCE.target("/v3/Data/test.ndjson").request().head();
        assertEquals(String.valueOf(length), head.getHeaderString(HttpHeaders.CONTENT_LENGTH));

        // HTTP range ends are inclusive
        Invocation.Builder requestBuilder = RESOURCE.target("/v3/Data/test.ndjson")
            .request()
            .header(org.apache.hc.core5.http.HttpHeaders.RANGE, "bytes=0-1")
            .header(org.apache.hc.core5.http.HttpHeaders.ACCEPT, "application/ndjson");

        for (Map.Entry<String, String> header : requestHeaders.entrySet()) {
            requestBuilder.header(header.getKey(), header.getValue());
        }
        Response response = requestBuilder.get();
        String responseBody = readResponse(response, false);

        assertEquals(HttpStatus.PARTIAL_CONTENT_206, response.getStatus(), "Should have partial content status");
        assertEquals(randomString.substring(0, 2), responseBody, "Should return both requested bytes");
        assertEquals("bytes 0-1/" + length, response.getHeaderString(CONTENT_RANGE));
        assertEquals("2", response.getHeaderString(HttpHeaders.CONTENT_LENGTH));

        response = RESOURCE.target("/v3/Data/test.ndjson").request()
            .header(RANGE, "bytes=0-0").get();
        assertEquals(HttpStatus.PARTIAL_CONTENT_206, response.getStatus());
        assertEquals(String.valueOf(randomString.charAt(0)), readResponse(response, false));
        assertEquals("bytes 0-0/" + length, response.getHeaderString(CONTENT_RANGE));
        assertEquals("1", response.getHeaderString(HttpHeaders.CONTENT_LENGTH));

        // Request 500 kb, with an offset
        int start = 30;
        int end = 500 * 1024 + start;
        requestBuilder = RESOURCE.target("/v3/Data/test.ndjson")
            .request()
            .header(org.apache.hc.core5.http.HttpHeaders.RANGE, String.format("bytes=%d-%s", start, end))
            .header(org.apache.hc.core5.http.HttpHeaders.ACCEPT, "application/ndjson");

        for (Map.Entry<String, String> header : requestHeaders.entrySet()) {
            requestBuilder.header(header.getKey(), header.getValue());
        }
        response = requestBuilder.get();
        responseBody = readResponse(response, false);

        assertEquals(HttpStatus.PARTIAL_CONTENT_206, response.getStatus(), "Should have partial content status");
        assertEquals(randomString.substring(start, end + 1), responseBody, "Response should match");
        assertEquals(String.format("bytes %d-%d/%d", start, end, length), response.getHeaderString(CONTENT_RANGE));
        assertEquals(String.valueOf(end - start + 1), response.getHeaderString(HttpHeaders.CONTENT_LENGTH));

        // Request the entire file
        requestBuilder = RESOURCE.target("/v3/Data/test.ndjson")
            .request()
            .header(org.apache.hc.core5.http.HttpHeaders.RANGE, String.format("bytes=%d-%s", 0, length))
            .header(org.apache.hc.core5.http.HttpHeaders.ACCEPT, "application/ndjson");

        for (Map.Entry<String, String> header : requestHeaders.entrySet()) {
            requestBuilder.header(header.getKey(), header.getValue());
        }
        response = requestBuilder.get();
        responseBody = readResponse(response, false);

        assertEquals(HttpStatus.PARTIAL_CONTENT_206, response.getStatus(), "Should have partial content status");
        assertEquals(randomString, responseBody, "Response should match");
        assertEquals(String.format("bytes 0-%d/%d", length - 1, length), response.getHeaderString(CONTENT_RANGE));
        assertEquals(String.valueOf(length), response.getHeaderString(HttpHeaders.CONTENT_LENGTH));

        // Request the entire file, without the ending value, which returns one chunk
        requestBuilder = RESOURCE.target("/v3/Data/test.ndjson")
            .request()
            .header(org.apache.hc.core5.http.HttpHeaders.RANGE, String.format("bytes=%d-", 0))
            .header(org.apache.hc.core5.http.HttpHeaders.ACCEPT, "application/ndjson");

        for (Map.Entry<String, String> header : requestHeaders.entrySet()) {
            requestBuilder.header(header.getKey(), header.getValue());
        }
        response = requestBuilder.get();
        responseBody = readResponse(response, false);

        assertEquals(HttpStatus.PARTIAL_CONTENT_206, response.getStatus(), "Should have partial content status");
        assertEquals(randomString.substring(0, 1024 * 1024), responseBody, "Response should match");
        assertEquals("bytes 0-1048575/" + length, response.getHeaderString(CONTENT_RANGE));
        assertEquals("1048576", response.getHeaderString(HttpHeaders.CONTENT_LENGTH));

        response = RESOURCE.target("/v3/Data/test.ndjson").request()
            .header(RANGE, "bytes=" + (length - 1) + "-").get();
        assertEquals(HttpStatus.PARTIAL_CONTENT_206, response.getStatus());
        assertEquals(String.valueOf(randomString.charAt(length - 1)), readResponse(response, false));
        assertEquals(String.format("bytes %d-%d/%d", length - 1, length - 1, length),
            response.getHeaderString(CONTENT_RANGE));
        assertEquals("1", response.getHeaderString(HttpHeaders.CONTENT_LENGTH));

        response = RESOURCE.target("/v3/Data/test.ndjson").request()
            .header(RANGE, "bytes=" + length + "-").get();
        assertEquals(Response.Status.REQUESTED_RANGE_NOT_SATISFIABLE.getStatusCode(), response.getStatus());
        assertEquals("bytes */" + length, response.getHeaderString(CONTENT_RANGE));

        // Request file with an invalid range
        requestBuilder = RESOURCE.target("/v3/Data/test.ndjson")
            .request()
            .header(org.apache.hc.core5.http.HttpHeaders.RANGE, "bytes=50-0")
            .header(org.apache.hc.core5.http.HttpHeaders.ACCEPT, "application/ndjson");
        for (Map.Entry<String, String> header : requestHeaders.entrySet()) {
            requestBuilder.header(header.getKey(), header.getValue());
        }
        response = requestBuilder.get();

        assertEquals(Response.Status.REQUESTED_RANGE_NOT_SATISFIABLE.getStatusCode(), response.getStatus());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void emptyFileHasNoSatisfiableRange(boolean legacyExtension, @TempDir Path directory) throws IOException {
        final Path path = directory.resolve(legacyExtension ? "empty.ndjson.gz" : "empty.ndjson");
        Files.write(path, GzipUtil.compress(""));
        when(manager.getV3File(Mockito.any(), Mockito.anyString())).thenReturn(
            new FileManager.FilePointer("", Files.size(path), UUID.randomUUID(),
                OffsetDateTime.now(ZoneOffset.UTC), path.toFile(), true)
        );

        final Response response = RESOURCE.target("/v3/Data/test.ndjson").request()
            .header(RANGE, "bytes=0-0").get();
        assertEquals(Response.Status.REQUESTED_RANGE_NOT_SATISFIABLE.getStatusCode(), response.getStatus());
        assertEquals("bytes */0", response.getHeaderString(CONTENT_RANGE));
    }

    @Test
    void rangeSpansConcatenatedGzipMembers(@TempDir Path directory) throws IOException {
        final Path path = directory.resolve("multiple.ndjson");
        Files.write(path, GzipUtil.compress("first"));
        Files.write(path, GzipUtil.compress("second"), StandardOpenOption.APPEND);
        when(manager.getV3File(Mockito.any(), Mockito.anyString())).thenReturn(
            new FileManager.FilePointer("", Files.size(path), UUID.randomUUID(),
                OffsetDateTime.now(ZoneOffset.UTC), path.toFile(), true)
        );

        final Response response = RESOURCE.target("/v3/Data/test.ndjson").request()
            .header(RANGE, "bytes=3-8").get();
        assertEquals(HttpStatus.PARTIAL_CONTENT_206, response.getStatus());
        assertEquals("stseco", readResponse(response, false));
        assertEquals("bytes 3-8/11", response.getHeaderString(CONTENT_RANGE));
        assertEquals("6", response.getHeaderString(HttpHeaders.CONTENT_LENGTH));
    }

    @Test
    void cachesUncompressedSizeAcrossRequestsAndInvalidatesOnChecksumChange(@TempDir Path directory) throws IOException {
        final Path path = directory.resolve("cached.ndjson");
        Files.write(path, GzipUtil.compress("test data"));
        FileManager.FilePointer pointer = spy(new FileManager.FilePointer("original", Files.size(path),
            UUID.randomUUID(), OffsetDateTime.now(ZoneOffset.UTC), path.toFile(), true));
        when(manager.getV3File(Mockito.any(), Mockito.anyString())).thenReturn(pointer);

        assertEquals("9", RESOURCE.target("/v3/Data/test.ndjson").request().head()
            .getHeaderString(HttpHeaders.CONTENT_LENGTH));
        assertEquals("9", RESOURCE.target("/v3/Data/test.ndjson").request().head()
            .getHeaderString(HttpHeaders.CONTENT_LENGTH));
        verify(pointer, times(1)).getUncompressedInputStream();

        FileManager.FilePointer updated = spy(new FileManager.FilePointer("updated", Files.size(path),
            UUID.randomUUID(), OffsetDateTime.now(ZoneOffset.UTC), path.toFile(), true));
        when(manager.getV3File(Mockito.any(), Mockito.anyString())).thenReturn(updated);
        assertEquals("9", RESOURCE.target("/v3/Data/test.ndjson").request().head()
            .getHeaderString(HttpHeaders.CONTENT_LENGTH));
        verify(updated, times(1)).getUncompressedInputStream();
    }

    @Test
    void testNonByteRange() {
        when(manager.getV3File(Mockito.any(), Mockito.anyString())).thenAnswer(answer -> {
            final File tempPath = FileUtils.getTempDirectory();
            final File file = File.createTempFile("test", ".ndjson", tempPath);
            FileUtils.writeByteArrayToFile(file, GzipUtil.compress("This is a test"));
            return new FileManager.FilePointer("", file.length(), UUID.randomUUID(), OffsetDateTime.now(ZoneOffset.UTC), file, true);
        });

        final Response response = RESOURCE.target("/v3/Data/test.ndjson")
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

            when(manager.getV3File(Mockito.any(), Mockito.anyString())).thenAnswer(answer -> {
                final File tempPath = FileUtils.getTempDirectory();
                final File file = File.createTempFile("test", ".ndjson", tempPath);
                FileUtils.writeByteArrayToFile(file, GzipUtil.compress("This is a test"));
                return new FileManager.FilePointer("This should match", file.length(), UUID.randomUUID(), modifiedDate, file, true);
            });
        }

        @HttpParamTest
        void testMissingETagHeader(String method) {
            final Invocation.Builder builder = RESOURCE.target("/v3/Data/test.ndjson")
                    .request();

            final Response response = createHTTPMethodCall(method, builder);

            assertAll(() -> assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), "Should have downloaded"));
        }

        @HttpParamTest
        void testMismatchingETagHeader(String method) {
            final Invocation.Builder builder = RESOURCE.target("/v3/Data/test.ndjson")
                    .request()
                    .header(org.apache.hc.core5.http.HttpHeaders.ACCEPT, "application/ndjson")
                    .header(HttpHeaders.IF_NONE_MATCH, "Not a real value");

            final Response response = createHTTPMethodCall(method, builder);

            assertAll(() -> assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), "Should have downloaded"));
        }

        @HttpParamTest
        void testCorrectETagHeader(String method) {
            final Invocation.Builder builder = RESOURCE.target("/v3/Data/test.ndjson")
                    .request()
                    .header(HttpHeaders.IF_NONE_MATCH, "This should match");

            final Response response = createHTTPMethodCall(method, builder);

            assertAll(() -> assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), response.getStatus(), "Should have downloaded"));
        }

        @HttpParamTest
        void testWeakETagHeader(String method) {
            final Invocation.Builder builder = RESOURCE.target("/v3/Data/test.ndjson")
                    .request()
                    .header(org.apache.hc.core5.http.HttpHeaders.ACCEPT, "application/ndjson")
                    .header(HttpHeaders.IF_NONE_MATCH, "This should match--gzip");

            final Response response = createHTTPMethodCall(method, builder);

            assertAll(() -> assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), response.getStatus(), "Should have downloaded"));
        }

        @HttpParamTest
        void testModifiedTimestamp(String method) {

            final Invocation.Builder builder = RESOURCE.target("/v3/Data/test.ndjson")
                    .request()
                    .header(org.apache.hc.core5.http.HttpHeaders.ACCEPT, "application/ndjson")
                    .header(HttpHeaders.IF_MODIFIED_SINCE, modifiedDate.toInstant().toEpochMilli());

            final Response response = createHTTPMethodCall(method, builder);

            assertAll(() -> assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), response.getStatus(), "Should have downloaded"));
        }

        @HttpParamTest
        void testMalformedModifiedTimestamp(String method) {
            final Invocation.Builder builder = RESOURCE.target("/v3/Data/test.ndjson")
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

        final DataResource dataResource = new DataResource(manager, client, queue,
            Caffeine.newBuilder().maximumSize(10).build());
        final FhirContext ctx = FhirContext.forR4();
        final StaticAuthFilter staticFilter = new StaticAuthFilter(new StaticAuthenticator());

        return APITestHelpers.buildResourceExtension(ctx,
                Collections.singletonList(new BaseResource(null, dataResource, null, null, null, null, null, null)),
                List.of(staticFilter,
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
