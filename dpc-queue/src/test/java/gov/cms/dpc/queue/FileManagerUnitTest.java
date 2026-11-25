package gov.cms.dpc.queue;

import gov.cms.dpc.common.utils.GzipUtil;
import gov.cms.dpc.queue.models.JobQueueBatch;
import gov.cms.dpc.queue.models.JobQueueBatchFile;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FileManagerUnitTest {
	private final String exportPath = "/tmp";
	private final String fileId = "fileId";
	private final UUID organizationID = UUID.randomUUID();
	private final UUID batchID = UUID.randomUUID();
	private final IJobQueue queue = mock(IJobQueue.class);

	@Test
	void getsCompressedFile() {
		String fileName = "fileName";
		JobQueueBatch batch = mock(JobQueueBatch.class);
		JobQueueBatchFile batchFile = mock(JobQueueBatchFile.class);

		when(queue.getJobBatchFile(organizationID, fileId)).thenReturn(Optional.of(batchFile));
		when(queue.getBatch(batchID)).thenReturn(Optional.of(batch));
		when(batchFile.getBatchID()).thenReturn(batchID);
		when(batchFile.getFileName()).thenReturn(fileName);
		when(batchFile.getChecksum()).thenReturn(new byte[] {1, 2, 3});
		when(batch.getStartTime()).thenReturn(Optional.of(OffsetDateTime.now()));

		FileManager fileManager = new FileManager(exportPath, queue);
		Path path = Paths.get(String.format("%s/%s.ndjson.gz", exportPath, fileName));

		try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
			mockedFiles.when(() -> Files.exists(path)).thenReturn(true);
			FileManager.FilePointer filePointer = fileManager.getFile(organizationID, fileId);
			assertEquals(fileName + ".ndjson.gz", filePointer.getFile().getName());
			assertTrue(filePointer.isCompressed());
		}
	}

	@Test
	void getsUnCompressedFile() {
		String fileName = "fileName";
		JobQueueBatch batch = mock(JobQueueBatch.class);
		JobQueueBatchFile batchFile = mock(JobQueueBatchFile.class);

		when(queue.getJobBatchFile(organizationID, fileId)).thenReturn(Optional.of(batchFile));
		when(queue.getBatch(batchID)).thenReturn(Optional.of(batch));
		when(batchFile.getBatchID()).thenReturn(batchID);
		when(batchFile.getFileName()).thenReturn(fileName);
		when(batchFile.getChecksum()).thenReturn(new byte[] {1, 2, 3});
		when(batch.getStartTime()).thenReturn(Optional.of(OffsetDateTime.now()));

		FileManager fileManager = new FileManager(exportPath, queue);
		Path path = Paths.get(String.format("%s/%s.ndjson", exportPath, fileName));

		try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
			mockedFiles.when(() -> Files.exists(path)).thenReturn(true);
			FileManager.FilePointer filePointer = fileManager.getFile(organizationID, fileId);
			assertEquals(fileName + ".ndjson", filePointer.getFile().getName());
			assertFalse(filePointer.isCompressed());
		}
	}

	@ParameterizedTest
	@ValueSource(strings = {".ndjson", ".ndjson.gz"})
	void r4ReadsCompressedFilesWithEitherExtension(String extension, @TempDir Path directory) throws IOException {
		String fileName = "fileName";
		String content = "test data";
		Path path = directory.resolve(fileName + extension);
		Files.write(path, GzipUtil.compress(content));
		JobQueueBatch batch = mock(JobQueueBatch.class);
		JobQueueBatchFile batchFile = mock(JobQueueBatchFile.class);
		when(queue.getJobBatchFile(organizationID, fileId)).thenReturn(Optional.of(batchFile));
		when(queue.getBatch(batchID)).thenReturn(Optional.of(batch));
		when(batchFile.getBatchID()).thenReturn(batchID);
		when(batchFile.getFileName()).thenReturn(fileName);
		when(batchFile.getChecksum()).thenReturn(new byte[] {1, 2, 3});
		when(batch.getStartTime()).thenReturn(Optional.of(OffsetDateTime.now()));

		FileManager fileManager = new FileManager(directory.toString(), queue);
		FileManager.FilePointer filePointer = fileManager.getV3File(organizationID, fileId);
		assertEquals(path.toFile(), filePointer.getFile());
		assertTrue(filePointer.isCompressed());
		try (InputStream stream = filePointer.getUncompressedInputStream()) {
			assertEquals(content, new String(stream.readAllBytes(), StandardCharsets.UTF_8));
		}
		if (extension.equals(".ndjson")) {
			assertFalse(fileManager.getFile(organizationID, fileId).isCompressed());
		}
	}

	@Test
	void handlesMissingBatchFile() {
		when(queue.getJobBatchFile(organizationID, fileId)).thenReturn(Optional.empty());
		FileManager fileManager = new FileManager(exportPath, queue);
		WebApplicationException e = assertThrows(WebApplicationException.class, () -> fileManager.getFile(organizationID, fileId));
		assertEquals(Response.Status.NOT_FOUND.getStatusCode(), e.getResponse().getStatus());
		WebApplicationException r4Exception = assertThrows(WebApplicationException.class,
			() -> fileManager.getV3File(organizationID, fileId));
		assertEquals(Response.Status.NOT_FOUND.getStatusCode(), r4Exception.getResponse().getStatus());
	}

	@Test
	void handlesMissingBatch() {
		JobQueueBatchFile batchFile = mock(JobQueueBatchFile.class);

		when(queue.getJobBatchFile(organizationID, fileId)).thenReturn(Optional.of(batchFile));
		when(queue.getBatch(batchID)).thenReturn(Optional.empty());

		FileManager fileManager = new FileManager(exportPath, queue);
		WebApplicationException e = assertThrows(WebApplicationException.class, () -> fileManager.getFile(organizationID, fileId));
		assertEquals(Response.Status.NOT_FOUND.getStatusCode(), e.getResponse().getStatus());
	}

	@Test
	void handlesMissingFile() {
		String fileName = "fileName";
		JobQueueBatch batch = mock(JobQueueBatch.class);
		JobQueueBatchFile batchFile = mock(JobQueueBatchFile.class);

		when(queue.getBatch(batchID)).thenReturn(Optional.of(batch));
		when(batchFile.getFileName()).thenReturn(fileName);
		when(batchFile.getBatchID()).thenReturn(batchID);

		FileManager fileManager = new FileManager(exportPath, queue);
		WebApplicationException e = assertThrows(WebApplicationException.class, () -> fileManager.getFile(batchFile));
		assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e.getResponse().getStatus());
	}

	@Test
	void filePointerGetters() {
		String checksum = "checksum";
		int fileSize = 1024;
		UUID jobID = UUID.randomUUID();
		OffsetDateTime creationTime = OffsetDateTime.now();
		File file = mock(File.class);
		boolean compressed = false;

		FileManager.FilePointer filePointer = new  FileManager.FilePointer(
			checksum,
			fileSize,
			jobID,
			creationTime,
			file,
			compressed
		);

		assertEquals(checksum, filePointer.getChecksum());
		assertEquals(fileSize, filePointer.getFileSize());
		assertEquals(jobID, filePointer.getJobID());
		assertEquals(creationTime, filePointer.getCreationTime());
		assertEquals(file, filePointer.getFile());
		assertEquals(compressed, filePointer.isCompressed());
	}

	@Test
	void filePointerGetsUncompressedStreamFromUncompressedFile() throws IOException {
		String testData = "test data";

		String checksum = "checksum";
		int fileSize = 1024;
		UUID jobID = UUID.randomUUID();
		OffsetDateTime creationTime = OffsetDateTime.now();
		boolean compressed = false;

		File tempPath = FileUtils.getTempDirectory();
		File file = File.createTempFile("test", ".ndjson", tempPath);
		FileUtils.write(file, testData, StandardCharsets.UTF_8);

		FileManager.FilePointer filePointer = new  FileManager.FilePointer(
			checksum,
			fileSize,
			jobID,
			creationTime,
			file,
			compressed
		);

		InputStream inputStream = filePointer.getUncompressedInputStream();
		assertEquals(testData, new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
	}

	@Test
	void filePointerGetsUncompressedStreamFromCompressedFile() throws IOException {
		String testData = "test data";

		String checksum = "checksum";
		int fileSize = 1024;
		UUID jobID = UUID.randomUUID();
		OffsetDateTime creationTime = OffsetDateTime.now();
		boolean compressed = true;

		File tempPath = FileUtils.getTempDirectory();
		File file = File.createTempFile("test", ".ndjson.gz", tempPath);
		FileUtils.writeByteArrayToFile(file, GzipUtil.compress(testData));

		FileManager.FilePointer filePointer = new  FileManager.FilePointer(
			checksum,
			fileSize,
			jobID,
			creationTime,
			file,
			compressed
		);

		InputStream inputStream = filePointer.getUncompressedInputStream();
		assertEquals(testData, new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
	}
}
