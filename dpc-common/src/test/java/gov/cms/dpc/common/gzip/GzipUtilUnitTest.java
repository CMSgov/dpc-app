package gov.cms.dpc.common.gzip;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

// Prevent parallel execution since all tests read and write the same temp file.
@Execution(ExecutionMode.SAME_THREAD)
class GzipUtilUnitTest {
	private final Path path = Paths.get("/tmp/GzipUtilUnitTest.tmp");

	@Test
	void canCheckCompressed() throws IOException {
		String data = "to_be_compressed".repeat(100);
		byte[] compressedData = GzipUtil.compress(data);

		assertTrue(GzipUtil.isCompressed(compressedData));
	}

	@Test
	void canCheckUncompressed_short() {
		// One byte isn't enough for a Gzip header
		byte[] shortData = {0};
		assertFalse(GzipUtil.isCompressed(shortData));
	}

	@Test
	void canCheckFileCompressed() throws IOException {
		byte[] compressedData = GzipUtil.compress("to_be_compressed".repeat(100));

		try {
			Files.write(path, compressedData);
		} catch (IOException e) {
			fail("Could not write temp file for test");
		}

		assertTrue(GzipUtil.isCompressed(path.toFile()));
	}

	@Test
	void canCheckFileNotCompressed() throws IOException {
		try {
			Files.writeString(path, "abcd");
		} catch (IOException e) {
			fail("Could not write temp file for test");
		}

		assertFalse(GzipUtil.isCompressed(path.toFile()));
	}

	@Test
	void canCheckShortFileNotCompressed() throws IOException {
		try {
			Files.writeString(path, "x");
		} catch (IOException e) {
			fail("Could not write temp file for test");
		}

		assertFalse(GzipUtil.isCompressed(path.toFile()));
	}

	@Test
	void canCompressAndDecompress() throws IOException {
		String data = "to_be_decompressed".repeat(100);
		byte[] compressedData = GzipUtil.compress(data);
		String decompressedData = GzipUtil.decompress(compressedData);
		assertEquals(data, decompressedData);
	}
}
