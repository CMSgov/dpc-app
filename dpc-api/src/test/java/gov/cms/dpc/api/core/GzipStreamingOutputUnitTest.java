package gov.cms.dpc.api.core;

import gov.cms.dpc.common.utils.GzipUtil;
import jakarta.ws.rs.WebApplicationException;
import org.apache.commons.io.input.BrokenInputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GzipStreamingOutputUnitTest {
	@Test
	void canWriteUncompressed() throws IOException {
		String data = "uncompressed data";
		GzipStreamingOutput gso = new GzipStreamingOutput(new ByteArrayInputStream(data.getBytes()), false);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		gso.write(baos);
		byte[] compressedData = baos.toByteArray();

		Assertions.assertEquals(data, GzipUtil.decompress(compressedData));
	}

	@Test
	void canWriteCompressed() throws IOException {
		String data = "compressed data".repeat(512);
		byte[] compressedDataIn = GzipUtil.compress(data);
		GzipStreamingOutput gso = new GzipStreamingOutput(new ByteArrayInputStream(compressedDataIn), true);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		gso.write(baos);
		byte[] compressedDataOut = baos.toByteArray();

		assertEquals(data, GzipUtil.decompress(compressedDataOut));
	}

	@Test
	void handlesIOException() {
		BrokenInputStream bis = new BrokenInputStream();
		GzipStreamingOutput gso = new GzipStreamingOutput(bis, false);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		assertThrows(WebApplicationException.class, () -> gso.write(baos));
	}
}
