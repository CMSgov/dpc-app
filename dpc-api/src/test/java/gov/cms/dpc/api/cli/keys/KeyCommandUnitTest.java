package gov.cms.dpc.api.cli.keys;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KeyCommandUnitTest {
    @Test
    public void testConstructor() {
        KeyCommand keyCommand = new KeyCommand();
        assertEquals("Public key related commands", keyCommand.getDescription());
        assertEquals("key", keyCommand.getName());
    }
}
