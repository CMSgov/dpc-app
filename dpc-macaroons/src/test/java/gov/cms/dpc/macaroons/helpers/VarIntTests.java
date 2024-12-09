package gov.cms.dpc.macaroons.helpers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.*;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Numeric handling")
class VarIntTests {

    private VarIntTests() {
    }

    @ParameterizedTest
    @DisplayName("Read signed long 🥳")
    @MethodSource("longStream")
    void testSignedLong(long value) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream(Long.BYTES);
        final DataOutputStream os = new DataOutputStream(bos);
        VarInt.writeSignedVarLong(value, os);

        final DataInputStream is = new DataInputStream(new ByteArrayInputStream(bos.toByteArray()));
        assertEquals(value, VarInt.readSignedVarLong(is), "Should be identical");
    }

    @ParameterizedTest
    @DisplayName("Read unsigned long 🥳")
    @MethodSource("longStream")
    void testUnsignedLong(long value) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream(Long.BYTES);
        final DataOutputStream os = new DataOutputStream(bos);
        VarInt.writeUnsignedVarLong(value, os);

        final DataInputStream is = new DataInputStream(new ByteArrayInputStream(bos.toByteArray()));
        assertEquals(value, VarInt.readUnsignedVarLong(is), "Should be identical");
    }

    @ParameterizedTest
    @DisplayName("Read signed int 🥳")
    @MethodSource("intStream")
    void testSignedInt(int value) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream(Integer.BYTES);
        final DataOutputStream os = new DataOutputStream(bos);
        VarInt.writeSignedVarInt(value, os);

        final DataInputStream is = new DataInputStream(new ByteArrayInputStream(bos.toByteArray()));
        assertEquals(value, VarInt.readSignedVarInt(is), "Should be identical");

        assertEquals(value, VarInt.readSignedVarInt(VarInt.writeSignedVarInt(value)), "Should be identical");
    }

    @ParameterizedTest
    @DisplayName("Read unsigned int 🥳")
    @MethodSource("intStream")
    void testUnsignedInt(int value) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream(Integer.BYTES);
        final DataOutputStream os = new DataOutputStream(bos);
        VarInt.writeUnsignedVarInt(value, os);

        final DataInputStream is = new DataInputStream(new ByteArrayInputStream(bos.toByteArray()));
        assertEquals(value, VarInt.readUnsignedVarInt(is), "Should be identical");

        assertEquals(value, VarInt.readUnsignedVarInt(VarInt.writeUnsignedVarInt(value)), "Should be identical");
    }

    @Test
    @DisplayName("Overflow stream 🤮")
    void testIntOverflow() throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream(Long.BYTES);
        final DataOutputStream os = new DataOutputStream(bos);
        VarInt.writeSignedVarLong(Long.MAX_VALUE, os);


        final DataInputStream is = new DataInputStream(new ByteArrayInputStream(bos.toByteArray()));
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> VarInt.readUnsignedVarInt(is), "Should overflow with long value");
        assertEquals(VarInt.VARIABLE_LENGTH_QUANTITY_IS_TOO_LONG, exception.getMessage(), "Should have correct message");

        //noinspection ResultOfMethodCallIgnored
        exception = assertThrows(IllegalArgumentException.class, () -> VarInt.readUnsignedVarInt(bos.toByteArray()), "Should overflow with long value");
        assertEquals(VarInt.VARIABLE_LENGTH_QUANTITY_IS_TOO_LONG, exception.getMessage(), "Should have correct message");
    }

    private static LongStream longStream() {
        return new Random().longs(50);
    }

    private static IntStream intStream() {
        return new Random().ints(50);
    }


}
