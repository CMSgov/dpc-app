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
	public static final String GZIP = "gzip";

	// A Gzipped file will start with these two "magic" numbers
	private static final int GZIP_MAGIC_1 = 0x1f;
	private static final int GZIP_MAGIC_2 = 0x8b;

	private GzipUtil() {
		// Not used
	}

	/**
	 * Checks if the given byte array is gzip compressed.
	 * @param data The data to check.
	 * @return true if the data is compressed, false otherwise.
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
	 * Checks if the {@link InputStream} is gzip compressed.  Note: The stream must be positioned at the start of the
	 * compressed data for it to be recognized.
	 * @param inputStream
	 * @return Returns true if the {@link InputStream} is pointing to the start of gzip compressed data.
	 * @throws IOException If there is a problem reading the {@link InputStream}.
	 */
	public static boolean isCompressed(InputStream inputStream) throws IOException {
		// Wrapping in a buffered stream so we can reset our cursor
		try (BufferedInputStream bis = new BufferedInputStream(inputStream)) {
			byte[] firstTwoBytes = new byte[2];

			// We need to read the first two bytes to determine if the input stream is compressed, but we don't want to
			// remove those bytes from the stream.
			bis.mark(2);
			int bytesRead = bis.read(firstTwoBytes);
			bis.reset();

			if (bytesRead != 2) {return false;}
			return isCompressed(firstTwoBytes);
		}
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
