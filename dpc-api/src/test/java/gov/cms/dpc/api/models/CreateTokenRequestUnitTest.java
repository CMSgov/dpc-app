package gov.cms.dpc.api.models;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
@DisplayName("Token handling")


class CreateTokenRequestUnitTest {

    @Test
@DisplayName("Override token request hashcode and equals 🥳")

    void testEqualsAndHashCode() {
        CreateTokenRequest token1 = new CreateTokenRequest();
        token1.setExpiresAt(OffsetDateTime.now());
        token1.setLabel("Test Label");

        CreateTokenRequest token2 = new CreateTokenRequest();
        token2.setExpiresAt(token1.getExpiresAt());
        token2.setLabel(token1.getLabel());

        assertTrue(token1.equals(token2), "Tokens should have been equal");
        assertTrue(token2.equals(token1), "Tokens should have been equal");
        assertTrue(token1.hashCode() == token2.hashCode(), "Hash code should match.");
        token2.setLabel("Different Test Label");
        assertFalse(token1.equals(token2), "Tokens should NOT have been equal");
        assertFalse(token2.equals(token1), "Tokens should NOT have been equal");
        assertFalse(token1.hashCode() == token2.hashCode(), "Hash code should NOT match.");


    }
}