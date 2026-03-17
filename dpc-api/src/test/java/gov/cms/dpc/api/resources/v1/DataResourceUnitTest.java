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
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.io.*;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
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

    @Test
    void streamingTest_uncompressedResponse_uncompressedFile() throws IOException {
        Mockito.when(manager.getFile(Mockito.any(), Mockito.anyString())).thenAnswer(answer -> {
            final File tempPath = FileUtils.getTempDirectory();
            final File file = File.createTempFile("test", ".ndjson", tempPath);
            FileUtils.write(file, "This is a test", StandardCharsets.UTF_8);
            return new FileManager.FilePointer("", file.length(), UUID.randomUUID(), OffsetDateTime.now(ZoneOffset.UTC), file, false);
        });

        final Response response = RESOURCE.target("/v1/Data/test.ndjson")
                .request()
                .header(org.apache.hc.core5.http.HttpHeaders.ACCEPT, "application/ndjson")
                .get();

        final InputStream output = response.readEntity(InputStream.class);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        IOUtils.copy(output, bos);
        assertAll(() -> assertEquals("This is a test", bos.toString(StandardCharsets.UTF_8), "Should have correct string"),
                () -> assertEquals(HttpStatus.OK_200, response.getStatus(), "Should have ok status"),
                () -> assertNotNull(response.getHeaderString(HttpHeaders.ETAG), "Should have eTag"));
    }

    @Test
    void streamingTest_uncompressedResponse_compressedFile() throws IOException {
        String testData = "This is a test";

        Mockito.when(manager.getFile(Mockito.any(), Mockito.anyString())).thenAnswer(answer -> {
            final File tempPath = FileUtils.getTempDirectory();
            final File file = File.createTempFile("test", ".ndjson.gz", tempPath);
            FileUtils.writeByteArrayToFile(file, GzipUtil.compress(testData));
            return new FileManager.FilePointer("", file.length(), UUID.randomUUID(), OffsetDateTime.now(ZoneOffset.UTC), file, true);
        });

        final Response response = RESOURCE.target("/v1/Data/test.ndjson")
            .request()
            .header(HttpHeaders.ACCEPT, "application/ndjson")
            .get();

        final InputStream output = response.readEntity(InputStream.class);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        IOUtils.copy(output, bos);
        String responseBody = bos.toString(StandardCharsets.UTF_8);
        assertAll(() -> assertEquals(testData, responseBody, "Should have correct string"),
            () -> assertEquals(HttpStatus.OK_200, response.getStatus(), "Should have ok status"),
            () -> assertNotNull(response.getHeaderString(HttpHeaders.ETAG), "Should have eTag"));
    }

    @Test
    void streamingTest_compressedResponse_uncompressedFile() throws IOException {
        Mockito.when(manager.getFile(Mockito.any(), Mockito.anyString())).thenAnswer(answer -> {
            final File tempPath = FileUtils.getTempDirectory();
            final File file = File.createTempFile("test", ".ndjson", tempPath);
            FileUtils.write(file, "This is a test", StandardCharsets.UTF_8);
            return new FileManager.FilePointer("", file.length(), UUID.randomUUID(), OffsetDateTime.now(ZoneOffset.UTC), file, false);
        });

        final Response response = RESOURCE.target("/v1/Data/test.ndjson")
            .request()
            .header(org.apache.hc.core5.http.HttpHeaders.ACCEPT, "application/ndjson")
            .acceptEncoding(GzipUtil.GZIP)
            .get();
        String responseBody = readCompressedResponse(response);

        assertAll(() -> assertEquals("This is a test", responseBody, "Should have correct string"),
            () -> assertEquals(HttpStatus.OK_200, response.getStatus(), "Should have ok status"),
            () -> assertNotNull(response.getHeaderString(HttpHeaders.ETAG), "Should have eTag"));
    }

    @Test
    void streamingTest_compressedResponse_compressedFile() throws IOException {
        Mockito.when(manager.getFile(Mockito.any(), Mockito.anyString())).thenAnswer(answer -> {
            final File tempPath = FileUtils.getTempDirectory();
            final File file = File.createTempFile("test", ".ndjson.gz", tempPath);
            FileUtils.writeByteArrayToFile(file, GzipUtil.compress("This is a test"));
            return new FileManager.FilePointer("", file.length(), UUID.randomUUID(), OffsetDateTime.now(ZoneOffset.UTC), file, true);
        });

        final Response response = RESOURCE.target("/v1/Data/test.ndjson")
            .request()
            .header(org.apache.hc.core5.http.HttpHeaders.ACCEPT, "application/ndjson")
            .acceptEncoding(GzipUtil.GZIP)
            .get();
        String responseBody = readCompressedResponse(response);

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

    @Test
    void testRangeRequest_uncompressedResponse_uncompressedFile() throws IOException {
        final File tempPath = FileUtils.getTempDirectory();
        final File file = File.createTempFile("test", ".ndjson", tempPath);
        final int length = 4 * 1024 * 1024;
        final String randomString = buildRandomString();
        FileUtils.write(file, randomString, StandardCharsets.UTF_8);

        Mockito.when(manager.getFile(Mockito.any(), Mockito.anyString())).thenReturn(new FileManager.FilePointer("", 0, UUID.randomUUID(), OffsetDateTime.now(ZoneOffset.UTC), file, false));

        // Try to request one byte
        Response response = RESOURCE.target("/v1/Data/test.ndjson")
                .request()
                .header(org.apache.hc.core5.http.HttpHeaders.RANGE, "bytes=0-1")
                .header(org.apache.hc.core5.http.HttpHeaders.ACCEPT, "application/ndjson")
                .get();

        InputStream is = response.readEntity(InputStream.class);
        StringWriter stringWriter = new StringWriter();
        IOUtils.copy(is, stringWriter, StandardCharsets.UTF_8);

        final String ch1 = response.getHeaderString(org.apache.hc.core5.http.HttpHeaders.CONTENT_LENGTH);
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
                .header(org.apache.hc.core5.http.HttpHeaders.RANGE, String.format("bytes=%d-%s", start, end))
                .get();

        is = response.readEntity(InputStream.class);
        stringWriter = new StringWriter();
        IOUtils.copy(is, stringWriter, StandardCharsets.UTF_8);

        final String ws2 = stringWriter.toString();
        assertEquals(randomString.substring(start, end), ws2, "Strings should match");
        stringWriter.getBuffer().setLength(0);

        // Request the entire file
        response = RESOURCE.target("/v1/Data/test.ndjson")
                .request()
                .header(org.apache.hc.core5.http.HttpHeaders.RANGE, String.format("bytes=0-%s", length))
                .get();

        is = response.readEntity(InputStream.class);
        stringWriter = new StringWriter();
        IOUtils.copy(is, stringWriter, StandardCharsets.UTF_8);

        final String ws3 = stringWriter.toString();
        assertEquals(randomString, ws3, "Strings should match");
        stringWriter.getBuffer().setLength(0);

        // Request the entire file, without the ending value, which returns one chunk
        response = RESOURCE.target("/v1/Data/test.ndjson")
                .request()
                .header(org.apache.hc.core5.http.HttpHeaders.RANGE, "bytes=0-")
                .get();

        is = response.readEntity(InputStream.class);
        stringWriter = new StringWriter();
        IOUtils.copy(is, stringWriter, StandardCharsets.UTF_8);

        final String ws4 = stringWriter.toString();
        assertEquals(randomString.substring(0, 1024 * 1024), ws4, "Should match the first 1MB of the file");

        // Request file with an invalid range
        response = RESOURCE.target("/v1/Data/test.ndjson")
                .request()
                .header(org.apache.hc.core5.http.HttpHeaders.ACCEPT, "application/ndjson")
                .header(org.apache.hc.core5.http.HttpHeaders.RANGE, "bytes=50-0")
                .get();

        assertEquals(Response.Status.REQUESTED_RANGE_NOT_SATISFIABLE.getStatusCode(), response.getStatus());
        assertEquals("{\"code\":416,\"message\":\"Range end cannot be before begin\"}", response.readEntity(String.class), "Should have correct status code");
    }

    @Test
    void testRangeRequest_compressedResponse_uncompressedFile() throws IOException {
        final File tempPath = FileUtils.getTempDirectory();
        final File file = File.createTempFile("test", ".ndjson", tempPath);
        final int length = 4 * 1024 * 1024;
        final String randomString = buildRandomString();
        FileUtils.write(file, randomString, StandardCharsets.UTF_8);

        Mockito.when(manager.getFile(Mockito.any(), Mockito.anyString())).thenReturn(new FileManager.FilePointer("", 0, UUID.randomUUID(), OffsetDateTime.now(ZoneOffset.UTC), file, false));

        // Try to request one byte
        Response response = RESOURCE.target("/v1/Data/test.ndjson")
            .request()
            .header(org.apache.hc.core5.http.HttpHeaders.RANGE, "bytes=0-1")
            .header(org.apache.hc.core5.http.HttpHeaders.ACCEPT, "application/ndjson")
            .acceptEncoding(GzipUtil.GZIP)
            .get();
        final String responseBody = readCompressedResponse(response);

        assertEquals(String.valueOf(randomString.charAt(0)), responseBody, "Should only have a single byte");

        // Request 500 kb, with an offset
        int start = 30;
        int end = 500 * 1024 + start;
        response = RESOURCE.target("/v1/Data/test.ndjson")
            .request()
            .header(org.apache.hc.core5.http.HttpHeaders.RANGE, String.format("bytes=%d-%s", start, end))
            .acceptEncoding(GzipUtil.GZIP)
            .get();
        final String responseBody2 = readCompressedResponse(response);

        assertEquals(randomString.substring(start, end), responseBody2, "Strings should match");

        // Request the entire file
        response = RESOURCE.target("/v1/Data/test.ndjson")
            .request()
            .header(org.apache.hc.core5.http.HttpHeaders.RANGE, String.format("bytes=0-%s", length))
            .acceptEncoding(GzipUtil.GZIP)
            .get();
        final String responseBody3 = readCompressedResponse(response);

        assertEquals(randomString, responseBody3, "Strings should match");

        // Request the entire file, without the ending value, which returns one chunk
        response = RESOURCE.target("/v1/Data/test.ndjson")
            .request()
            .header(org.apache.hc.core5.http.HttpHeaders.RANGE, "bytes=0-")
            .acceptEncoding(GzipUtil.GZIP)
            .get();
        final String responseBody4 = readCompressedResponse(response);

        assertEquals(randomString.substring(0, 1024 * 1024), responseBody4, "Should match the first 1MB of the file");

        // Request file with an invalid range
        response = RESOURCE.target("/v1/Data/test.ndjson")
            .request()
            .header(org.apache.hc.core5.http.HttpHeaders.ACCEPT, "application/ndjson")
            .header(org.apache.hc.core5.http.HttpHeaders.RANGE, "bytes=50-0")
            .acceptEncoding(GzipUtil.GZIP)
            .get();

        assertEquals(Response.Status.REQUESTED_RANGE_NOT_SATISFIABLE.getStatusCode(), response.getStatus());
        assertEquals("{\"code\":416,\"message\":\"Range end cannot be before begin\"}", response.readEntity(String.class), "Should have correct status code");
    }

    @Test
    void testRangeRequest_uncompressedResponse_compressedFile() throws IOException {
        final File tempPath = FileUtils.getTempDirectory();
        final File file = File.createTempFile("test", ".ndjson.gz", tempPath);
        final int length = 4 * 1024 * 1024;
        final String randomString = buildRandomString();
        FileUtils.writeByteArrayToFile(file, GzipUtil.compress(randomString));

        Mockito.when(manager.getFile(Mockito.any(), Mockito.anyString())).thenReturn(new FileManager.FilePointer("", 0, UUID.randomUUID(), OffsetDateTime.now(ZoneOffset.UTC), file, true));

        // Try to request one byte
        Response response = RESOURCE.target("/v1/Data/test.ndjson")
            .request()
            .header(org.apache.hc.core5.http.HttpHeaders.RANGE, "bytes=0-1")
            .header(org.apache.hc.core5.http.HttpHeaders.ACCEPT, "application/ndjson")
            .get();

        InputStream is = response.readEntity(InputStream.class);
        StringWriter stringWriter = new StringWriter();
        IOUtils.copy(is, stringWriter, StandardCharsets.UTF_8);

        final String ws1 = stringWriter.toString();
        assertEquals(String.valueOf(randomString.charAt(0)), ws1, "Should only have a single byte");
        stringWriter.getBuffer().setLength(0);

        // Request 500 kb, with an offset
        int start = 30;
        int end = 500 * 1024 + start;
        response = RESOURCE.target("/v1/Data/test.ndjson")
            .request()
            .header(org.apache.hc.core5.http.HttpHeaders.RANGE, String.format("bytes=%d-%s", start, end))
            .get();

        is = response.readEntity(InputStream.class);
        stringWriter = new StringWriter();
        IOUtils.copy(is, stringWriter, StandardCharsets.UTF_8);

        final String ws2 = stringWriter.toString();
        assertEquals(randomString.substring(start, end), ws2, "Strings should match");
        stringWriter.getBuffer().setLength(0);

        // Request the entire file
        response = RESOURCE.target("/v1/Data/test.ndjson")
            .request()
            .header(org.apache.hc.core5.http.HttpHeaders.RANGE, String.format("bytes=0-%s", length))
            .get();

        is = response.readEntity(InputStream.class);
        stringWriter = new StringWriter();
        IOUtils.copy(is, stringWriter, StandardCharsets.UTF_8);

        final String ws3 = stringWriter.toString();
        assertEquals(randomString, ws3, "Strings should match");
        stringWriter.getBuffer().setLength(0);

        // Request the entire file, without the ending value, which returns one chunk
        response = RESOURCE.target("/v1/Data/test.ndjson")
            .request()
            .header(org.apache.hc.core5.http.HttpHeaders.RANGE, "bytes=0-")
            .get();

        is = response.readEntity(InputStream.class);
        stringWriter = new StringWriter();
        IOUtils.copy(is, stringWriter, StandardCharsets.UTF_8);

        final String ws4 = stringWriter.toString();
        assertEquals(randomString.substring(0, 1024 * 1024), ws4, "Should match the first 1MB of the file");

        // Request file with an invalid range
        response = RESOURCE.target("/v1/Data/test.ndjson")
            .request()
            .header(org.apache.hc.core5.http.HttpHeaders.ACCEPT, "application/ndjson")
            .header(org.apache.hc.core5.http.HttpHeaders.RANGE, "bytes=50-0")
            .get();

        assertEquals(Response.Status.REQUESTED_RANGE_NOT_SATISFIABLE.getStatusCode(), response.getStatus());
        assertEquals("{\"code\":416,\"message\":\"Range end cannot be before begin\"}", response.readEntity(String.class), "Should have correct status code");
    }

    @Test
    void testRangeRequest_compressedResponse_compressedFile() throws IOException {
        final File tempPath = FileUtils.getTempDirectory();
        final File file = File.createTempFile("test", ".ndjson.gz", tempPath);
        final int length = 4 * 1024 * 1024;
        final String randomString = buildRandomString();
        FileUtils.writeByteArrayToFile(file, GzipUtil.compress(randomString));

        Mockito.when(manager.getFile(Mockito.any(), Mockito.anyString())).thenReturn(new FileManager.FilePointer("", 0, UUID.randomUUID(), OffsetDateTime.now(ZoneOffset.UTC), file, true));

        // Try to request one byte
        Response response = RESOURCE.target("/v1/Data/test.ndjson")
            .request()
            .header(org.apache.hc.core5.http.HttpHeaders.RANGE, "bytes=0-1")
            .header(org.apache.hc.core5.http.HttpHeaders.ACCEPT, "application/ndjson")
            .acceptEncoding(GzipUtil.GZIP)
            .get();
        final String responseBody = readCompressedResponse(response);

        assertEquals(String.valueOf(randomString.charAt(0)), responseBody, "Should only have a single byte");

        // Request 500 kb, with an offset
        int start = 30;
        int end = 500 * 1024 + start;
        response = RESOURCE.target("/v1/Data/test.ndjson")
            .request()
            .header(org.apache.hc.core5.http.HttpHeaders.RANGE, String.format("bytes=%d-%s", start, end))
            .acceptEncoding(GzipUtil.GZIP)
            .get();
        final String responseBody2 = readCompressedResponse(response);

        assertEquals(randomString.substring(start, end), responseBody2, "Strings should match");

        // Request the entire file
        response = RESOURCE.target("/v1/Data/test.ndjson")
            .request()
            .header(org.apache.hc.core5.http.HttpHeaders.RANGE, String.format("bytes=0-%s", length))
            .acceptEncoding(GzipUtil.GZIP)
            .get();
        final String responseBody3 = readCompressedResponse(response);

        assertEquals(randomString, responseBody3, "Strings should match");

        // Request the entire file, without the ending value, which returns one chunk
        response = RESOURCE.target("/v1/Data/test.ndjson")
            .request()
            .header(org.apache.hc.core5.http.HttpHeaders.RANGE, "bytes=0-")
            .acceptEncoding(GzipUtil.GZIP)
            .get();
        final String responseBody4 = readCompressedResponse(response);

        assertEquals(randomString.substring(0, 1024 * 1024), responseBody4, "Should match the first 1MB of the file");

        // Request file with an invalid range
        response = RESOURCE.target("/v1/Data/test.ndjson")
            .request()
            .header(org.apache.hc.core5.http.HttpHeaders.ACCEPT, "application/ndjson")
            .header(org.apache.hc.core5.http.HttpHeaders.RANGE, "bytes=50-0")
            .acceptEncoding(GzipUtil.GZIP)
            .get();

        assertEquals(Response.Status.REQUESTED_RANGE_NOT_SATISFIABLE.getStatusCode(), response.getStatus());
        assertEquals("{\"code\":416,\"message\":\"Range end cannot be before begin\"}", response.readEntity(String.class), "Should have correct status code");
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

    @Retention(RetentionPolicy.RUNTIME)
    @ParameterizedTest(name = "{arguments} request")
    @ValueSource(strings = {"HEAD", "GET"})
    @interface HttpParamTest {
    }
}
