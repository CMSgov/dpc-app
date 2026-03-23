package gov.cms.dpc.api.core;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.zip.GZIPInputStream;

/**
 * Ensures that the given {@link File} is not compressed, and writes it to an {@link OutputStream} so it can be returned
 * in a {@link jakarta.ws.rs.core.Response}.
 */
public class UnGzipStreamingOutput implements StreamingOutput {
	private final InputStream inputStream;
	private final boolean isCompressed;

	public UnGzipStreamingOutput(InputStream inputStream, boolean isCompressed) {
		this.inputStream = inputStream;
		this.isCompressed = isCompressed;
	}

	@Override
	public void write(OutputStream outputStream) throws IOException, WebApplicationException {
		try {
			// If the file's not compressed we can write it as is.  If it is, we have to decompress first.
			if (!isCompressed) {
				IOUtils.copy(inputStream, outputStream);
			} else {
				GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream);
				IOUtils.copy(gzipInputStream, outputStream);
			}
			outputStream.flush();
		} catch (IOException e) {
			throw new WebApplicationException("Unable to write uncompressed stream", e, Response.Status.INTERNAL_SERVER_ERROR);
		}
	}
}
