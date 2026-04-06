package gov.cms.dpc.api.core;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Implements {@link StreamingOutput} on an {@link InputStream} that may or may not be gzip compressed, and can write it
 * either gzip compressed or not.
 */
public class CompressibleStreamingOutput implements StreamingOutput {
	private final InputStream inputStream;
	private final boolean isCompressed;
	private final boolean compressOutput;

	public CompressibleStreamingOutput(InputStream inputStream, boolean isCompressed, boolean compressOutput) {
		this.inputStream = inputStream;
		this.isCompressed = isCompressed;
		this.compressOutput = compressOutput;
	}

	@Override
	public void write(OutputStream outputStream) throws IOException, WebApplicationException {
		try {
			if (compressOutput && !isCompressed) {
				// Input file is not compressed, but we want to compress our response
				GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream);
				IOUtils.copy(inputStream, gzipOutputStream);
				gzipOutputStream.finish();
			} else if (!compressOutput && isCompressed) {
				// Input file is compressed, but we want an uncompressed response
				GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream);
				IOUtils.copy(gzipInputStream, outputStream);
			} else {
				// Input and output are both either compressed or not compressed.  Just copy it over.
				IOUtils.copy(inputStream, outputStream);
			}
			outputStream.flush();
		} catch (IOException e) {
			String message = compressOutput
				? "Unable to write compressed stream"
				: "Unable to write uncompressed stream";
			throw new WebApplicationException(message, e, Response.Status.INTERNAL_SERVER_ERROR);
		}
	}
}
