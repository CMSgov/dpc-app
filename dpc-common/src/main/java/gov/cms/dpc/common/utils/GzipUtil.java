package gov.cms.dpc.common.utils;

import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Utility class to Gzip compression and decompression.
 */
public class GzipUtil {
	public static final String GZIP = "gzip";

	private GzipUtil() {
		// Not used
	}

	/**
	 * Decompresses gzipped data into a {@link String}.
	 * @param in Data alreadys compressed with Gzip.
	 * @return The data converted to a {@link String}
	 * @throws IOException If the data isn't gzip compressed.
	 */
	public static String decompress(byte[] in) throws IOException {
		try (ByteArrayInputStream bais = new ByteArrayInputStream(in);
			 GZIPInputStream gzis = new GZIPInputStream(bais)) {
			return IOUtils.toString(gzis, StandardCharsets.UTF_8);
		}
	}

	/**
	 * Decompresses a file and returns its contents as a {@link String}.
	 * @param inputFile Location of the file to be read.
	 * @return Uncompressed file
	 * @throws IOException If it can't read the file
	 */
	public static String decompress(String inputFile) throws IOException {
		try (
			FileInputStream fis = new FileInputStream(inputFile);
			GZIPInputStream gzis = new GZIPInputStream(fis);
		) {
			return IOUtils.toString(gzis, StandardCharsets.UTF_8);
		}
	}

	/**
	 * Compresses the {@link String}.
	 * @param in The {@link String} to be compressed.
	 * @return The gzip compressed data.
	 * @throws IOException If there's a problem compressing/writing the data.
	 */
	public static byte[] compress(String in) throws IOException {
		try(ByteArrayOutputStream baos = new ByteArrayOutputStream();
			GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
			gzos.write(in.getBytes(StandardCharsets.UTF_8));
			gzos.finish();
			return baos.toByteArray();
		}
	}
}
