package gov.cms.dpc.common.gzip;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Ensures that the given {@link File} is gzip compressed and writes it to an {@link OutputStream} so it can be returned
 * in a {@link jakarta.ws.rs.core.Response}.
 */
public class GzipStreamingOutput implements StreamingOutput {
	private final File file;

	public GzipStreamingOutput(File file) {
		this.file = file;
	}

	@Override
	public void write(OutputStream outputStream) throws IOException, WebApplicationException {
		try (FileInputStream fileInputStream = new FileInputStream(file)) {

			// If the file's already compressed, just stream it.  If not, compress it first.
			if (GzipUtil.isCompressed(file)) {
				IOUtils.copy(fileInputStream, outputStream);
			} else {
				try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream)) {
					IOUtils.copy(fileInputStream, gzipOutputStream);
					gzipOutputStream.finish();
				}
			}
		} catch (IOException e) {
			throw new WebApplicationException(String.format("Unable to read/compress file %s", file.getName()), e, Response.Status.INTERNAL_SERVER_ERROR);
		}
	}
}
