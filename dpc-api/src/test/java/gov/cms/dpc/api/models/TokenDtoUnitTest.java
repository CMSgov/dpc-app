package gov.cms.dpc.api.models;

import org.junit.jupiter.api.Test;
import org.pitest.reloc.antlr.common.StringUtils;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TokenDtoUnitTest {

    @Test
    void testEqualsAndHashCode() {
        TokenDto token1 = new TokenDto();
        token1.setCreatedAt(OffsetDateTime.now());
        token1.setExpiresAt(OffsetDateTime.now());
        token1.setId("Test Id");
        token1.setLabel("Test Label");
        token1.setOrganizationID(UUID.randomUUID());
        token1.setToken("test token");
        token1.setTokenType("token Type");

        TokenDto token2 = new TokenDto();
        token2.setCreatedAt(token1.getCreatedAt());
        token2.setExpiresAt(token1.getExpiresAt());
        token2.setId(token1.getId());
        token2.setLabel(token1.getLabel());
        token2.setOrganizationID(token1.getOrganizationID());
        token2.setToken(token1.getToken());
        token2.setTokenType(token1.getTokenType());

        assertTrue(token1.equals(token2), "Tokens should have been equal");
        assertTrue(token2.equals(token1), "Tokens should have been equal");
        assertTrue(token1.hashCode() == token2.hashCode(), "Hash code should match.");
        token2.setLabel("Different Test Label");
        assertFalse(token1.equals(token2), "Tokens should NOT have been equal");
        assertFalse(token2.equals(token1), "Tokens should NOT have been equal");
        assertFalse(token1.hashCode() == token2.hashCode(), "Hash code should NOT match.");


    }
}