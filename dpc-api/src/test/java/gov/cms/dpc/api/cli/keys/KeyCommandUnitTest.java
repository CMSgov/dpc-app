package gov.cms.dpc.api.cli.keys;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
@DisplayName("Public key command submission")


class KeyCommandUnitTest {
    @Test
    @DisplayName("Key commands 🥳")
public void testConstructor() {
        KeyCommand keyCommand = new KeyCommand();
        assertEquals("Public key related commands", keyCommand.getDescription());
        assertEquals("key", keyCommand.getName());
    }
}
