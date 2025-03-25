package gov.cms.dpc.api.models;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CreateTokenRequestUnitTest {

    @Test
    void testEqualsAndHashCode() {
        CreateTokenRequest token1 = new CreateTokenRequest();
        token1.setExpiresAt(OffsetDateTime.now());
        token1.setLabel("Test Label");

        CreateTokenRequest token2 = new CreateTokenRequest();
        token2.setExpiresAt(token1.getExpiresAt());
        token2.setLabel(token1.getLabel());

        assertEquals(token1, token2, "Tokens should have been equal");
        assertEquals(token2, token1, "Tokens should have been equal");
        assertEquals(token1.hashCode(), token2.hashCode(), "Hash code should match.");
        token2.setLabel("Different Test Label");
        assertNotEquals(token1, token2, "Tokens should NOT have been equal");
        assertNotEquals(token2, token1, "Tokens should NOT have been equal");
        assertNotEquals(token1.hashCode(), token2.hashCode(), "Hash code should NOT match.");
    }
}
