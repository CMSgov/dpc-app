package gov.cms.dpc.api.jdbi;

import gov.cms.dpc.api.entities.TokenEntity;
import gov.cms.dpc.common.hibernate.auth.DPCAuthManagedSessionFactory;
import gov.cms.dpc.testing.AbstractDAOTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TokenDAOUnitTest extends AbstractDAOTest<TokenEntity> {
    TokenDAO tokenDAO;

    @BeforeEach
    public void setUp() {
        tokenDAO = new TokenDAO(new DPCAuthManagedSessionFactory(db.getSessionFactory()));
    }

    @Test
    void writesToken() {
        UUID orgId = UUID.randomUUID();
        String tokenLabel = "test label";
        TokenEntity tokenEntity = createTokenEntity(orgId, tokenLabel);
        TokenEntity returnedToken = db.inTransaction(() -> tokenDAO.persistToken(tokenEntity));

        assertEquals(orgId, returnedToken.getOrganizationID());
        assertEquals(tokenLabel, returnedToken.getLabel());
        assertFalse(returnedToken.getCreatedAt().toString().isEmpty());
        assertFalse(returnedToken.getId().isEmpty());
        assertFalse(returnedToken.getOrganizationID().toString().isEmpty());
    }

    @Test
    void fetchesToken() {
        UUID orgId1 = UUID.randomUUID();
        UUID orgId2 = UUID.randomUUID();

        db.inTransaction(() -> {
            tokenDAO.persistToken(createTokenEntity(orgId1, "label 1"));
            tokenDAO.persistToken(createTokenEntity(orgId1, "label 2"));
            tokenDAO.persistToken(createTokenEntity(orgId2, "label 3"));
        });

        List<TokenEntity> results = db.inTransaction(() -> tokenDAO.fetchTokens(orgId1));

        assertEquals(2, results.size());

        TokenEntity org1Token = results.get(0);
        assertEquals(orgId1, org1Token.getOrganizationID());
        assertEquals("label 1", org1Token.getLabel());

        TokenEntity org2Token = results.get(1);
        assertEquals(orgId1, org2Token.getOrganizationID());
        assertEquals("label 2", org2Token.getLabel());
    }

    @Test
    void deletesToken() {
        UUID orgId = UUID.randomUUID();

        TokenEntity persistedToken = db.inTransaction(() ->
                tokenDAO.persistToken(createTokenEntity(orgId, "label 1")));

        List<TokenEntity> results = db.inTransaction(() -> {
            tokenDAO.deleteToken(persistedToken);
            return tokenDAO.fetchTokens(orgId);
        });

        assertEquals(0, results.size());
    }

    private TokenEntity createTokenEntity(UUID orgId, String label) {
        String tokenId = UUID.randomUUID().toString();
        TokenEntity newEntity = new TokenEntity(tokenId, orgId, TokenEntity.TokenType.MACAROON);
        newEntity.setLabel(label);
        return newEntity;
    }
}
