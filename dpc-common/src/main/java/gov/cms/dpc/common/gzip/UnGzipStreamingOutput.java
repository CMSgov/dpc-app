package gov.cms.dpc.common.gzip;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;

/**
 * Ensures that the given {@link File} is not compressed, and writes it to an {@link OutputStream} so it can be returned
 * in a {@link jakarta.ws.rs.core.Response}.
 */
public class UnGzipStreamingOutput implements StreamingOutput {
	private final File file;

	public UnGzipStreamingOutput(File file) {
		this.file = file;
	}

	@Override
	public void write(OutputStream outputStream) throws IOException, WebApplicationException {
		try (FileInputStream fileInputStream = new FileInputStream(file)) {
			// If the file's not compressed we can write it as is.  If it is, we have to decompress first.
			if( !GzipUtil.isCompressed(file) ) {
				IOUtils.copy(fileInputStream, outputStream);
			} else {
				try (GZIPInputStream gzipInputStream = new GZIPInputStream(fileInputStream)) {
					IOUtils.copy(gzipInputStream, outputStream);
				}
			}
		} catch (IOException e) {
			throw new WebApplicationException(String.format("Unable to read/decompress file %s", file.getName()), e, Response.Status.INTERNAL_SERVER_ERROR);
		}
	}
}
