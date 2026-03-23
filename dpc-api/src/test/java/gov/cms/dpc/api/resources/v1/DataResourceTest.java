package gov.cms.dpc.api.resources.v1;

import com.codahale.metrics.MetricRegistry;
import gov.cms.dpc.aggregation.util.AggregationUtils;
import gov.cms.dpc.api.APITestHelpers;
import gov.cms.dpc.api.AbstractSecureApplicationTest;
import gov.cms.dpc.common.utils.GzipUtil;
import gov.cms.dpc.common.hibernate.queue.DPCQueueManagedSessionFactory;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.queue.DistributedBatchQueue;
import gov.cms.dpc.queue.models.JobQueueBatch;
import gov.cms.dpc.queue.models.JobQueueBatchFile;
import gov.cms.dpc.testing.APIAuthHelpers;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.net.URIBuilder;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
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
			Arguments.of(false, Collections.EMPTY_MAP),	// uncompressed file, uncompressed download
			Arguments.of(false, Map.of(HttpHeaders.ACCEPT_ENCODING, GzipUtil.GZIP)),	// uncompressed file, compressed download
			Arguments.of(true, Collections.EMPTY_MAP), 	// compressed file, uncompressed download
			Arguments.of(true, Map.of(HttpHeaders.ACCEPT_ENCODING, GzipUtil.GZIP))	// compressed file, compressed download
		);
	}

	@ParameterizedTest
	@MethodSource("downloadArgs")
	void canDownloadFiles(boolean compressFile, Map<String, String> requestHeaders) throws IOException {
		String testData = "test data".repeat(100);
		String fileName = createTestExport(testData, compressFile);

		try (ClassicHttpResponse response = downloadExport(fileName, requestHeaders)) {
			assertNotNull(response);

			boolean expectGzipCompressed =
				requestHeaders.containsKey(HttpHeaders.ACCEPT_ENCODING) && requestHeaders.get(HttpHeaders.ACCEPT_ENCODING).contains("gzip");

			// Check content encoding
			Header contentEncoding = response.getHeader(HttpHeaders.CONTENT_ENCODING);
			if (expectGzipCompressed) {
				assertEquals(GzipUtil.GZIP, contentEncoding.getValue());
			} else {
				assertNull(contentEncoding);
			}

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
		} catch (IOException | ProtocolException e) {
			fail("Failed to read response");
		}
	}

	@ParameterizedTest
	@MethodSource("downloadArgs")
	void canDownloadFileRanges(boolean compressFile, Map<String, String> requestHeadersIn) throws IOException {
		String testData = "uncompressed_data";
		String fileName = createTestExport("12345_" + testData + "_54321", compressFile);

		// Add range header
		Map<String, String> requestHeaders = new HashMap<>(requestHeadersIn);
		requestHeaders.put(HttpHeaders.RANGE, String.format("bytes=%d-%d", 6, 6 + testData.length()));

		boolean expectGzipCompressed =
			requestHeaders.containsKey(HttpHeaders.ACCEPT_ENCODING) && requestHeaders.get(HttpHeaders.ACCEPT_ENCODING).contains("gzip");

		try (ClassicHttpResponse response = downloadExport(fileName, requestHeaders))
		{
			assertNotNull(response);

			// Check content encoding
			Header contentEncoding = response.getHeader(HttpHeaders.CONTENT_ENCODING);
			if (expectGzipCompressed) {
				assertEquals(GzipUtil.GZIP, contentEncoding.getValue());
			} else {
				assertNull(contentEncoding);
			}

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
		} catch (IOException | ProtocolException e) {
			fail("Failed to read response");
		}
	}

	// Downloads a file from the Data endpoint.
	// Turns off auto-handling of gzip compression so each test can handle it itself.
	private ClassicHttpResponse downloadExport(String fileName, Map<String, String> headers) {
		// Create client and call Data end point
		try {
			APIAuthHelpers.AuthResponse auth = APIAuthHelpers.jwtAuthFlow(getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY);

			try (final CloseableHttpClient client = APIAuthHelpers.createCustomHttpClient().disableCompression().trusting().build()) {
				final URIBuilder builder = new URIBuilder(String.format("%s/Data/%s", getBaseURL(), fileName));
				final HttpGet httpGet = new HttpGet(builder.build());
				httpGet.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + auth.accessToken);

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
	private String createTestExport(String content, boolean compress) throws IOException {
		DPCResourceType resourceType = DPCResourceType.Patient;
		int sequence = 0;
		UUID jobID = UUID.randomUUID();

		// Create batch
		final var submittedJobQueueBatch = new JobQueueBatch(
			jobID,
			UUID.fromString(APITestHelpers.ORGANIZATION_ID),
			APITestHelpers.ORGANIZATION_ID,
			"providerNpi",
			List.of(),
			List.of(resourceType),
			OffsetDateTime.now(),
			OffsetDateTime.now(),
			"reqIp",
			"reqUrl",
			true
		);
		submittedJobQueueBatch.addJobQueueFile(DPCResourceType.Patient, sequence, 100);

		// Add the new batch to the queue
		queue.submitJobBatches(List.of(submittedJobQueueBatch));

		// We created one job with one batch,
		List<JobQueueBatch> batches = queue.getJobBatches(jobID);
		JobQueueBatch batch = batches.get(0);
		JobQueueBatchFile batchFile = batch.getJobQueueBatchFiles().get(0);

		// Create the file in the file system and get its metadata
		String fileName = batchFile.getFileName() + ".ndjson";

		Path path;
		byte[] fileData;
		if  (compress) {
			path = Paths.get(exportPath + "/" + fileName + ".gz");
			fileData = GzipUtil.compress(content);
		} else {
			path = Paths.get(exportPath + "/" + fileName);
			fileData = content.getBytes(StandardCharsets.UTF_8);
		}

		try {
			Files.write(path, fileData);
			batchFile.setFileLength(content.length());
			try (FileInputStream fis = new FileInputStream(path.toFile())) {
				batchFile.setChecksum(AggregationUtils.generateChecksum(fis));
			}
		} catch (IOException e) {
			fail("Could not create test file: " + path);
		}

		// Complete the batch and save metadata changes
		UUID aggregatorID = UUID.randomUUID();
		batch.setRunningStatus(aggregatorID);
		queue.completeBatch(batch, aggregatorID);

		return fileName;
	}
}
