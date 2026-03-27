package gov.cms.dpc.api.core;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.zip.GZIPOutputStream;

/**
 * Ensures that the given {@link InputStream} is gzip compressed and writes it to an {@link OutputStream} so it can be returned
 * in a {@link jakarta.ws.rs.core.Response}.
 */
public class GzipStreamingOutput implements StreamingOutput {
	private final InputStream inputStream;
	private final boolean isCompressed;

	public GzipStreamingOutput(InputStream inputStream, boolean isCompressed) {
		this.inputStream = inputStream;
		this.isCompressed = isCompressed;
	}

	@Override
	public void write(OutputStream outputStream) throws IOException, WebApplicationException {
		try {
			// If the file's already compressed, just stream it.  If not, compress it first.
			if (isCompressed) {
				IOUtils.copy(inputStream, outputStream);
			} else {
				GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream);
				IOUtils.copy(inputStream, gzipOutputStream);
				gzipOutputStream.finish();
			}
			outputStream.flush();
		} catch (IOException e) {
			throw new WebApplicationException("Unable to write compressed stream", e, Response.Status.INTERNAL_SERVER_ERROR);
		}
	}
}
