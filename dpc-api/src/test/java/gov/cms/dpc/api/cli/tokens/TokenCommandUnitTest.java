package gov.cms.dpc.api.cli.tokens;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;


@DisplayName("Token command submission")
public class TokenCommandUnitTest {
    @Test
    @DisplayName("Client token commands 🥳")
public void testConstructor() {
        TokenCommand tokenCommand = new TokenCommand();
        assertEquals("Token related commands", tokenCommand.getDescription());
        assertEquals("token", tokenCommand.getName());
    }
}
