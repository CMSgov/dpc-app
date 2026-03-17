package gov.cms.dpc.common.gzip;

import jakarta.ws.rs.WebApplicationException;
import org.apache.commons.io.input.BrokenInputStream;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UnGzipStreamingOutputUnitTest {
	@Test
	void canWriteUncompressed() throws IOException {
		String data = "uncompressed data";
		UnGzipStreamingOutput ugso = new UnGzipStreamingOutput(new ByteArrayInputStream(data.getBytes()), false);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ugso.write(baos);

		assertEquals(data, baos.toString());
	}

	@Test
	void canWriteCompressed() throws IOException {
		String data = "compressed data";
		byte[] compressedDataIn = GzipUtil.compress(data);
		UnGzipStreamingOutput ugso = new UnGzipStreamingOutput(new ByteArrayInputStream(compressedDataIn), true);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ugso.write(baos);

		assertEquals(data, baos.toString());
	}

	@Test
	void handlesIOException() {
		BrokenInputStream bis = new BrokenInputStream();
		UnGzipStreamingOutput ugso = new UnGzipStreamingOutput(bis, true);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		assertThrows(WebApplicationException.class, () -> ugso.write(baos));
	}
}
