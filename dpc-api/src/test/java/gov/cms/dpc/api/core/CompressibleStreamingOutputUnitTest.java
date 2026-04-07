package gov.cms.dpc.api.core;

import gov.cms.dpc.common.utils.GzipUtil;
import jakarta.ws.rs.WebApplicationException;
import org.apache.commons.io.input.BrokenInputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class CompressibleStreamingOutputUnitTest {
	@Test
	void compressedOutputUncompressedSource() throws IOException {
		String data = "uncompressed data";
		CompressibleStreamingOutput compressibleStreamingOutput = new CompressibleStreamingOutput(
			new ByteArrayInputStream(data.getBytes()),
			false,
			true
		);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		compressibleStreamingOutput.write(baos);
		byte[] compressedData = baos.toByteArray();

		Assertions.assertEquals(data, GzipUtil.decompress(compressedData));
	}

	@Test
	void compressedOutputCompressedSource() throws IOException {
		String data = "compressed data".repeat(512);
		byte[] compressedDataIn = GzipUtil.compress(data);
		CompressibleStreamingOutput compressibleStreamingOutput = new CompressibleStreamingOutput(
			new ByteArrayInputStream(compressedDataIn),
			true,
			true
		);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		compressibleStreamingOutput.write(baos);
		byte[] compressedDataOut = baos.toByteArray();

		assertEquals(data, GzipUtil.decompress(compressedDataOut));
	}

	@Test
	void uncompressedOutputUncompressedSource() throws IOException {
		String data = "uncompressed data";
		CompressibleStreamingOutput compressibleStreamingOutput = new CompressibleStreamingOutput(
			new ByteArrayInputStream(data.getBytes()),
			false,
			false
		);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		compressibleStreamingOutput.write(baos);

		assertEquals(data, baos.toString());
	}

	@Test
	void uncompressedOutputCompressedSource() throws IOException {
		String data = "compressed data";
		byte[] compressedDataIn = GzipUtil.compress(data);
		CompressibleStreamingOutput compressibleStreamingOutput = new CompressibleStreamingOutput(
			new ByteArrayInputStream(compressedDataIn),
			true,
			false
		);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		compressibleStreamingOutput.write(baos);

		assertEquals(data, baos.toString());
	}

	@Test
	void handlesIOExceptionCompressedOutput() {
		CompressibleStreamingOutput compressibleStreamingOutput = new CompressibleStreamingOutput(
			new BrokenInputStream(),
			true,
			true
		);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		WebApplicationException exception = assertThrows(
			WebApplicationException.class, () -> compressibleStreamingOutput.write(baos)
		);
		assertEquals("Unable to write compressed stream", exception.getMessage());
	}

	@Test
	void handlesIOExceptionUncompressedOutput() {
		CompressibleStreamingOutput compressibleStreamingOutput = new CompressibleStreamingOutput(
			new BrokenInputStream(),
			false,
			false
		);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		WebApplicationException exception = assertThrows(
			WebApplicationException.class, () -> compressibleStreamingOutput.write(baos)
		);
		assertEquals("Unable to write uncompressed stream", exception.getMessage());
	}
}
