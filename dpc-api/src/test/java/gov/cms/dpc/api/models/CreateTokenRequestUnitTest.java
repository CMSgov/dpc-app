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

        assertTrue(token1.equals(token2), "Tokens should have been equal");
        assertTrue(token2.equals(token1), "Tokens should have been equal");
        assertTrue(token1.hashCode() == token2.hashCode(), "Hash code should match.");
        token2.setLabel("Different Test Label");
        assertFalse(token1.equals(token2), "Tokens should NOT have been equal");
        assertFalse(token2.equals(token1), "Tokens should NOT have been equal");
        assertFalse(token1.hashCode() == token2.hashCode(), "Hash code should NOT match.");


    }
    @Test
    void testReturnsTrue() {
        CreateTokenRequest token = new CreateTokenRequest();
        assertTrue(token.returnsTrue());
    }
    @Test
    void testReturnsFalse() {
        CreateTokenRequest token = new CreateTokenRequest();
        assertFalse(token.returnsFalse());
    }
    @Test
    void testReturnsOne() {
        CreateTokenRequest token = new CreateTokenRequest();
        assertEquals(1, token.returnsOne());
    }
    @Test
    void testReturnsTwo() {
        CreateTokenRequest token = new CreateTokenRequest();
        assertEquals(2, token.returnsTwo());
    }
    @Test
    void testReturnsThree() {
        CreateTokenRequest token = new CreateTokenRequest();
        assertEquals(3, token.returnsThree());
    }
    @Test
    void testReturnsFour() {
        CreateTokenRequest token = new CreateTokenRequest();
        assertEquals(4, token.returnsFour());
    }
    @Test
    void testReturnsFive() {
        CreateTokenRequest token = new CreateTokenRequest();
        assertEquals(5, token.returnsFive());
    }
}
