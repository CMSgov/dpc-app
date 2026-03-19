package gov.cms.dpc.common.gzip;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class GzipUtilUnitTest {
	@Test
	void canCheckCompressed() throws IOException {
		String data = "to_be_compressed".repeat(100);
		byte[] compressedData = GzipUtil.compress(data);

		assertTrue(GzipUtil.isCompressed(compressedData));
	}

	@Test
	void canCheckNotCompressed() {
		String data = "not_compressed";
		assertFalse(GzipUtil.isCompressed(data.getBytes()));
	}

	@Test
	void canCheckNotcompressed_short() {
		// One byte isn't enough for a Gzip header
		byte[] shortData = {0};
		assertFalse(GzipUtil.isCompressed(shortData));
	}

	@Test
	void canCheckStreamCompressed() throws IOException {
		byte[] compressedData = GzipUtil.compress("to_be_compressed".repeat(100));
		ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
		assertTrue(GzipUtil.isCompressed(bais));
	}

	@Test
	void canCheckStreamNotCompressed() throws IOException {
		ByteArrayInputStream bais = new ByteArrayInputStream("not_compressed".getBytes());
		assertFalse(GzipUtil.isCompressed(bais));
	}

	@Test
	void canCheckStreamNotCompressed_short() throws IOException {
		ByteArrayInputStream bais = new ByteArrayInputStream("x".getBytes());
		assertFalse(GzipUtil.isCompressed(bais));
	}

	@Test
	void canCompressAndDecompress() throws IOException {
		String data = "to_be_decompressed".repeat(100);
		byte[] compressedData = GzipUtil.compress(data);
		String decompressedData = GzipUtil.decompress(compressedData);
		assertEquals(data, decompressedData);
	}

	@Test
	void canDecompressfile() throws IOException {
		String data = "compressed_data";

		File tempPath = FileUtils.getTempDirectory();
		File file = File.createTempFile("test", ".ndjson.gz", tempPath);
		FileUtils.writeByteArrayToFile(file, GzipUtil.compress(data));

		String uncompressed = GzipUtil.decompress(file.getAbsolutePath());
		assertEquals(data, uncompressed);
	}
}
