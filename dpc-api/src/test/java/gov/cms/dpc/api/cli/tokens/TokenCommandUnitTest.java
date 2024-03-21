package gov.cms.dpc.api.cli.tokens;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;


public class TokenCommandUnitTest {
    @Test
    public void testConstructor() {
        TokenCommand tokenCommand = new TokenCommand();
        assertEquals("Token related commands", tokenCommand.getDescription());
        assertEquals("token", tokenCommand.getName());
    }
}
