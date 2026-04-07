package gov.cms.dpc.common.utils;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GzipUtilUnitTest {
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
