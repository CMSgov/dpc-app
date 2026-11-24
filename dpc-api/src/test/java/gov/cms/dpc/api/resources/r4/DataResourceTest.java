package gov.cms.dpc.api.resources.r4;

import com.codahale.metrics.MetricRegistry;
import gov.cms.dpc.aggregation.util.AggregationUtils;
import gov.cms.dpc.api.r4.APITestHelpers;
import gov.cms.dpc.api.r4.AbstractSecureApplicationTest;
import gov.cms.dpc.common.hibernate.queue.DPCQueueManagedSessionFactory;
import gov.cms.dpc.common.utils.GzipUtil;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.queue.DistributedBatchQueue;
import gov.cms.dpc.queue.models.JobQueueBatch;
import gov.cms.dpc.queue.models.JobQueueBatchFile;
import gov.cms.dpc.testing.APIAuthHelpers;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.net.URIBuilder;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class DataResourceTest extends AbstractSecureApplicationTest {

	private static DistributedBatchQueue queue;
	private final String exportPath = getExportPath();

	@BeforeAll
	static void configDb() {
		// Connect to our queue DB
		final Configuration conf = new Configuration().configure("hibernate-queue.cfg.xml");
		SessionFactory sessionFactory = conf.buildSessionFactory();
		queue = new DistributedBatchQueue(new DPCQueueManagedSessionFactory(sessionFactory), 100, new MetricRegistry());
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
	void canDownloadFiles(boolean legacyExtension, Map<String, String> requestHeaders) throws IOException, ProtocolException {
		String testData = "test data".repeat(50);
		String fileName = createTestExport(testData, legacyExtension);

		String token = getAccessToken();
		waitForFileReady(fileName, token);

		try (ClassicHttpResponse response = downloadExport(fileName, token, requestHeaders)) {
			assertNotNull(response);

			boolean expectGzipCompressed =
				requestHeaders.containsKey(HttpHeaders.ACCEPT_ENCODING) && requestHeaders.get(HttpHeaders.ACCEPT_ENCODING).contains("gzip");

			// Check headers
			Header contentEncoding = response.getHeader(HttpHeaders.CONTENT_ENCODING);
			Header contentLength = response.getHeader(HttpHeaders.CONTENT_LENGTH);
			Header transferEncoding = response.getHeader(HttpHeaders.TRANSFER_ENCODING);

			// A gzip'd response should have a content-encoding header
			if (expectGzipCompressed) {
				assertEquals(GzipUtil.GZIP, contentEncoding.getValue());
			} else {
				assertNull(contentEncoding);
			}

			// We should have content-length or transfer-encoding, but not both
			assertTrue((contentLength != null) ^ (transferEncoding != null));

			// Make sure we can read the body
			HttpEntity entity = response.getEntity();
			byte[] responseData = entity.getContent().readAllBytes();

			String responseString;
			if (expectGzipCompressed) {
				responseString = GzipUtil.decompress(responseData);
			} else {
				responseString = new String(responseData);
			}

			assertEquals(testData, responseString);
		}
	}

	@ParameterizedTest
	@MethodSource("downloadArgs")
	void canDownloadFileRanges(boolean legacyExtension, Map<String, String> requestHeadersIn) throws IOException, ProtocolException {
		String testData = "uncompressed_data";
		String fullContent = "12345_" + testData + "_54321";
		String fileName = createTestExport(fullContent, legacyExtension);

		String token = getAccessToken();
		waitForFileReady(fileName, token);

		// Add range header
		Map<String, String> requestHeaders = new HashMap<>(requestHeadersIn);
		requestHeaders.put(HttpHeaders.RANGE, String.format("bytes=%d-%d", 6, 6 + testData.length() - 1));

		try (ClassicHttpResponse response = downloadExport(fileName, token, requestHeaders))
		{
			assertNotNull(response);

			// Check headers
			Header contentEncoding = response.getHeader(HttpHeaders.CONTENT_ENCODING);
			Header contentLength = response.getHeader(HttpHeaders.CONTENT_LENGTH);
			Header transferEncoding = response.getHeader(HttpHeaders.TRANSFER_ENCODING);

			assertNull(contentEncoding);

			// We should have content-length or transfer-encoding, but not both
			assertTrue((contentLength != null) ^ (transferEncoding != null));

			// Make sure we can read the body
			HttpEntity entity = response.getEntity();
			byte[] responseData = entity.getContent().readAllBytes();
			String responseString = new String(responseData);
			assertEquals(testData, responseString);
			assertEquals(String.format("bytes 6-%d/%d", 6 + testData.length() - 1, fullContent.length()),
				response.getHeader(HttpHeaders.CONTENT_RANGE).getValue());
			assertEquals(testData.length(), responseData.length);
		}
	}

	// Downloads a file from the Data endpoint.
	// Turns off auto-handling of gzip compression so each test can verify the data is or isn't compressed.
	private ClassicHttpResponse downloadExport(String fileName, String token, Map<String, String> headers) {
		// Create client and call Data end point
		try {
			try (final CloseableHttpClient client = APIAuthHelpers.createCustomHttpClient().disableCompression().trusting().build()) {
				final URIBuilder builder = new URIBuilder(String.format("%s/Data/%s", getBaseURL(), fileName));
				final HttpGet httpGet = new HttpGet(builder.build());
				httpGet.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

				for (Map.Entry<String, String> header : headers.entrySet()) {
					httpGet.addHeader(header.getKey(), header.getValue());
				}

				return client.execute(httpGet, response -> response);
			}
		} catch (IOException | URISyntaxException e) {
			fail("Failed downloading export file");
		}
		return null;
	}

	// Creates a fake export file we can use the DataResource to download
	private String createTestExport(String content, boolean legacyExtension) throws IOException {
		DPCResourceType resourceType = DPCResourceType.Patient;
		int sequence = 0;
		UUID jobID = UUID.randomUUID();

		// Create batch
		final var submittedJobQueueBatch = new JobQueueBatch.Builder()
			.jobID(jobID)
			.orgID(UUID.fromString(APITestHelpers.ORGANIZATION_ID))
			.orgNPI(APITestHelpers.ORGANIZATION_ID)
			.providerNPI("providerNpi")
			.patients(List.of())
			.resourceTypes(List.of(resourceType))
			.since(OffsetDateTime.now())
			.transactionTime(OffsetDateTime.now())
			.requestingIP("reqIp")
			.requestUrl("reqUrl")
			.isBulk(true)
			.build();
		JobQueueBatchFile batchFile = submittedJobQueueBatch.addJobQueueFile(DPCResourceType.Patient, sequence, 100);

		// Create the file in the file system and get its metadata
		String fileName = batchFile.getFileName() + ".ndjson";

		Path path = Paths.get(exportPath + "/" + fileName + (legacyExtension ? ".gz" : ""));
		byte[] fileData = GzipUtil.compress(content);

		try {
			Files.write(path, fileData);
			batchFile.setFileLength(fileData.length);
			batchFile.setChecksum(AggregationUtils.generateChecksum(path.toFile()));
		} catch (IOException e) {
			fail("Could not create test file: " + path);
		}

		// Persist the complete file metadata before marking the batch complete
		queue.submitJobBatches(List.of(submittedJobQueueBatch));
		List<JobQueueBatch> batches = queue.getJobBatches(jobID);
		JobQueueBatch batch = batches.get(0);

		UUID aggregatorID = UUID.randomUUID();
		batch.setRunningStatus(aggregatorID);
		queue.completeBatch(batch, aggregatorID);

		return fileName;
	}

	// We're having intermittent test failures because the file isn't ready when we attempt to download it.  This
	// continuously makes HEAD requests to get the file metadata until we get a successful response.  Then it lets the
	// test proceed.
	private void waitForFileReady(String fileName, String token) {
		try {
			Awaitility.await()
				.atMost(java.time.Duration.ofSeconds(10))
				.pollInterval(java.time.Duration.ofMillis(200))
				.until(() -> {
					try (final CloseableHttpClient client = APIAuthHelpers.createCustomHttpClient().disableCompression().trusting().build()) {
						final URIBuilder builder = new URIBuilder(String.format("%s/Data/%s", getBaseURL(), fileName));
						final HttpHead httpHead = new HttpHead(builder.build());
						httpHead.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
						return client.execute(httpHead, response -> response.getCode() == HttpStatus.SC_OK);
					} catch (Exception e) {
						// On fail try again
						return false;
					}
				});
		} catch (Exception e) {
			fail("File not ready for download: " + e.getMessage());
		}
	}

	private String getAccessToken() {
		try {
			APIAuthHelpers.AuthResponse auth = APIAuthHelpers.jwtAuthFlow(getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY);
			return auth.accessToken;
		} catch (IOException | URISyntaxException e) {
			fail("Failed to generate access token");
			return null;
		}
	}
}
