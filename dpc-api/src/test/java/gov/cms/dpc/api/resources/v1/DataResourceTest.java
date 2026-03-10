package gov.cms.dpc.api.resources.v1;

import com.codahale.metrics.MetricRegistry;
import gov.cms.dpc.aggregation.util.AggregationUtils;
import gov.cms.dpc.api.APITestHelpers;
import gov.cms.dpc.api.AbstractSecureApplicationTest;
import gov.cms.dpc.common.hibernate.queue.DPCQueueManagedSessionFactory;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.queue.DistributedBatchQueue;
import gov.cms.dpc.queue.models.JobQueueBatch;
import gov.cms.dpc.queue.models.JobQueueBatchFile;
import gov.cms.dpc.testing.APIAuthHelpers;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.net.URIBuilder;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DataResourceTest extends AbstractSecureApplicationTest {

	private static DistributedBatchQueue queue;
	private final String exportPath = getExportPath();
	private final String gZIP = "gzip";

	@BeforeAll
	static void configDb() {
		// Connect to our queue DB
		final Configuration conf = new Configuration().configure("hibernate-queue.cfg.xml");
		SessionFactory sessionFactory = conf.buildSessionFactory();
		queue = new DistributedBatchQueue(new DPCQueueManagedSessionFactory(sessionFactory), 100, new MetricRegistry());
	}

	@Test
	void canDownloadUncompressed() {
		String fileName = createTestExport("uncompressed_data");

		try (ClassicHttpResponse response = downloadExport(fileName, false)) {
			assertNotNull(response);

			// Check content encoding
			Header contentEncoding = response.getHeader(HttpHeaders.CONTENT_ENCODING);
			assertNull(contentEncoding);

			// Make sure we can read the body
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				String responseString = EntityUtils.toString(entity);
				assertEquals("uncompressed_data", responseString);
			}
		} catch (IOException | ProtocolException E) {
			fail("Failed to read response");
		}
	}

	@Test
	@Disabled("Will fail until we manually add compression back")
	void canDownloadCompressed() {
		// A response needs to be at least 256 bytes before Dropwizard will compress it
		String expectedBody = "compressed_data".repeat(1000);
		String fileName = createTestExport(expectedBody);

		try (ClassicHttpResponse response = downloadExport(fileName, true)) {
			assertNotNull(response);

			// String test = EntityUtils.toString(response.getEntity());

			// Check content encoding
			Header contentEncoding = response.getHeader(HttpHeaders.CONTENT_ENCODING);
			assertEquals(gZIP, contentEncoding.getValue());

			// Make sure we can read the body
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				byte[] responseBytes = EntityUtils.toByteArray(entity);
				String decompressedResponseString = decompress(responseBytes);
				assertEquals(expectedBody, decompressedResponseString);
			}
		} catch (IOException | ProtocolException e) {
			fail("Failed to read response");
		}
	}

	// Downloads a file from the Data endpoint.
	// Turns off auto-handling of gzip compression so each test can handle it itself.
	private ClassicHttpResponse downloadExport(String fileName, Boolean compressed) {
		// Create client and call Data end point
		try {
			APIAuthHelpers.AuthResponse auth = APIAuthHelpers.jwtAuthFlow(getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY);

			 try (final CloseableHttpClient client = APIAuthHelpers.createCustomHttpClient().disableCompression().trusting().build()) {
				final URIBuilder builder = new URIBuilder(String.format("%s/Data/%s", getBaseURL(), fileName));
				final HttpGet httpGet = new HttpGet(builder.build());
				httpGet.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + auth.accessToken);

				// Add gzip compression
				if (compressed) {
					httpGet.addHeader(HttpHeaders.ACCEPT_ENCODING, gZIP);
				}

				return client.execute(httpGet, response -> response);
			}
		} catch (IOException | URISyntaxException e) {
			fail("Failed downloading export file");
		}
		return null;
	}

	// Creates a fake export file we can use the DataResource to download
	private String createTestExport(String content) {
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
		Path path = Paths.get(exportPath + "/" + fileName);
		try {
			Files.writeString(path, content);

			batchFile.setFileLength(Files.size(path));
			batchFile.setChecksum(AggregationUtils.generateChecksum(path.toFile()));
		} catch (IOException e) {
			fail();
		}

		// Complete the batch and save metadata changes
		UUID aggregatorID = UUID.randomUUID();
		batch.setRunningStatus(aggregatorID);
		queue.completeBatch(batch, aggregatorID);

		return fileName;
	}

	// Manually decompresses a gzip'd string so we can check its value
	private String decompress(byte[] in) throws IOException {
		try (ByteArrayInputStream bais = new ByteArrayInputStream(in);
			 GzipCompressorInputStream gzis = new GzipCompressorInputStream(bais)) {
				return IOUtils.toString(gzis, StandardCharsets.UTF_8);
		}
	}
}
