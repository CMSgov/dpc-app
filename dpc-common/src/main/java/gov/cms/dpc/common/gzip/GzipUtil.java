package gov.cms.dpc.common.gzip;

import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Utility class to Gzip compression and decompression.
 */
public class GzipUtil {
	// A Gzipped file will start with these two "magic" numbers
	private static final int GZIP_MAGIC_1 = 0x1f;
	private static final int GZIP_MAGIC_2 = 0x8b;

	private GzipUtil() {
		// Not used
	}

	/**
	 * Checks if the given file is gzip compressed.
	 * @param file The file to check.
	 * @return true if the file is compressed, false otherwise.
	 * @throws IOException If there is a problem reading the file.
	 */
	public static boolean isCompressed(File file) throws IOException {
		if (file.length() < 2) {
			return false;
		}

		try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
			// Check if we start with the magic bytes
			int b1 = is.read();
			int b2 = is.read();

			return (b1 == GZIP_MAGIC_1) && (b2 == GZIP_MAGIC_2);
		}
	}

	/**
	 * Checks if the given byte array is gzip compressed.
	 * @param data The data to check.
	 * @return true if the data is compressed, false otherwise.
	 * @throws IOException If there is a problem reading the data.
	 */
	public static boolean isCompressed(byte[] data) {
		if (data.length < 2) {
			return false;
		}

		int byte1 = Byte.toUnsignedInt(data[0]);
		int byte2 = Byte.toUnsignedInt(data[1]);
		return (byte1 == GZIP_MAGIC_1) &&  (byte2 == GZIP_MAGIC_2);
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
