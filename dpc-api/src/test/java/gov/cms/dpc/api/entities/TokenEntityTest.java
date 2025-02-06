package gov.cms.dpc.api.entities;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TokenEntityTest {

    @Test
    void testGettersAndSetters() {
        TokenEntity token = new TokenEntity();
        String id = UUID.randomUUID().toString();
        UUID orgId = UUID.randomUUID();
        TokenEntity.TokenType tokenType = TokenEntity.TokenType.MACAROON;
        String label = "label string";
        OffsetDateTime createdAt = OffsetDateTime.now();
        OffsetDateTime expiresAt = OffsetDateTime.now();
        String tokenStr = "token string";

        token.setId(id);
        token.setOrganizationID(orgId);
        token.setTokenType(tokenType);
        token.setLabel(label);
        token.setCreatedAt(createdAt);
        token.setExpiresAt(expiresAt);
        token.setToken(tokenStr);

        assertEquals(id, token.getId());
        assertEquals(orgId, token.getOrganizationID());
        assertEquals(tokenType, token.getTokenType());
        assertEquals(label, token.getLabel());
        assertEquals(createdAt, token.getCreatedAt());
        assertEquals(expiresAt, token.getExpiresAt());
        assertEquals(tokenStr, token.getToken());
    }
}
