package gov.cms.dpc.macaroons;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CaveatParsingTests {

    @Test
    void testSimpleCaveatParsing() {
        final MacaroonCaveat macaroonCaveat = MacaroonCaveat.parseFromString("test = valid");
        assertAll(() -> assertEquals("test", macaroonCaveat.getKey(), "Key should match"),
                () -> assertEquals(MacaroonCaveat.Operator.EQ, macaroonCaveat.getOp(), "Op should match"),
                () -> assertEquals("valid", macaroonCaveat.getValue(), "Value should match"));
    }

    @Test
    void testPoorlyFormattedCaveatParsing() {
        // Test spaces
        assertAll(() -> assertThrows(IllegalArgumentException.class, () -> MacaroonCaveat.parseFromString("test =valid"), "Caveats need spaces between entities"),
                () -> assertThrows(IllegalArgumentException.class, () -> MacaroonCaveat.parseFromString("test=valid"), "Caveats need spaces between entities"),
                () -> assertThrows(IllegalArgumentException.class, () -> MacaroonCaveat.parseFromString("test= valid"), "Caveats need spaces between entities"));
    }

    @Test
    void testInvalidOperationValueParsing() {
        assertAll(() -> assertThrows(IllegalArgumentException.class, () -> MacaroonCaveat.parseFromString("test ~ valid"), "Should throw for invalid operation type"));
    }

    @Test
    void testMalformedCaveatParsing() {
        assertThrows(IllegalArgumentException.class, () -> MacaroonCaveat.parseFromString("test ="), "Should not parse malformed caveat");
        assertThrows(IllegalArgumentException.class, () -> MacaroonCaveat.parseFromString("test id = hello"), "Should not parse caveat with strings in key");
    }
}
