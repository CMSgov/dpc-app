package gov.cms.dpc.macaroons.helpers;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class BackedInputStreamTests {

    @Test
    void testSimpleRead() throws IOException {
        final String testString = "Test String";
        final ByteBuffer bb = ByteBuffer.wrap(testString.getBytes(StandardCharsets.UTF_8));
        final ByteBufferBackedInputStream bis = new ByteBufferBackedInputStream(bb);

        final byte[] outputBytes = ByteBuffer.allocate(testString.length()).array();

        final int readBytes = bis.read(outputBytes, 0, testString.length());
        assertAll(() -> assertEquals(testString.length(), readBytes, "Should have matching number of bytes"),
                () -> assertEquals(testString, new String(outputBytes, StandardCharsets.UTF_8), "Should have correct buffer value"));
    }

    @Test
    void testEmptyBuffer() throws IOException {
        final ByteBufferBackedInputStream bis = new ByteBufferBackedInputStream(ByteBuffer.allocate(0));
        assertEquals(-1, bis.read(), "Should return empty size value");
    }

    @Test
    void testFullyReadBuffer() throws IOException {
        final ByteBufferBackedInputStream bis = new ByteBufferBackedInputStream(ByteBuffer.allocate(0));
        final byte[] outputArray = ByteBuffer.allocate(1).array();
        assertEquals(-1, bis.read(outputArray, 0, 1), "Should return empty size value");
    }

    @Test
    void testBufferOverflow() {
        final String testString = "Test String";
        final ByteBuffer bb = ByteBuffer.wrap(testString.getBytes(StandardCharsets.UTF_8));
        final ByteBufferBackedInputStream bis = new ByteBufferBackedInputStream(bb);

        final byte[] outputBytes = ByteBuffer.allocate(testString.length() - 1).array();

        assertThrows(IndexOutOfBoundsException.class, () -> bis.read(outputBytes, 0, testString.length()), "Cannot read out of bounds");
    }
}
