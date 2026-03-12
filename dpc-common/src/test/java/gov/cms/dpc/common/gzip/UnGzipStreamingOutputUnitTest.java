package gov.cms.dpc.common.gzip;

import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

// Prevent parallel execution since all tests read and write the same temp file.
@Execution(ExecutionMode.SAME_THREAD)
class UnGzipStreamingOutputUnitTest {
	private final Path path = Paths.get("/tmp/UnGzipStreamingOutputUnitTest.tmp");

	@Test
	void canWriteUncompressed() throws IOException {
		String data = "uncompressed data";
		try {
			Files.writeString(path, data);
		} catch (IOException e) {
			fail("Could not write temp file for test");
		}

		UnGzipStreamingOutput ugso = new UnGzipStreamingOutput(path.toFile());
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		ugso.write(baos);
		assertEquals(data, baos.toString());
	}

	@Test
	void canWriteCompressed() throws IOException {
		String data = "compressed data".repeat(512);
		byte[] compressedDataIn = GzipUtil.compress(data);

		try {
			Files.write(path, compressedDataIn);
		} catch (IOException e) {
			fail("Could not write temp file for test");
		}

		UnGzipStreamingOutput ugso = new UnGzipStreamingOutput(path.toFile());
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		ugso.write(baos);
		assertEquals(data, baos.toString());
	}

	@Test
	void handlesIOException() {
		UnGzipStreamingOutput ugso = new UnGzipStreamingOutput(new File("fake/file"));
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		assertThrows(WebApplicationException.class, () -> ugso.write(baos));
	}
}
