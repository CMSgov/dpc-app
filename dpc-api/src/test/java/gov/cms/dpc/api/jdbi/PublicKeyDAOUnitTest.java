package gov.cms.dpc.api.jdbi;

import gov.cms.dpc.api.entities.PublicKeyEntity;
import gov.cms.dpc.common.hibernate.auth.DPCAuthManagedSessionFactory;
import gov.cms.dpc.testing.APIAuthHelpers;
import gov.cms.dpc.testing.AbstractDAOTest;
import gov.cms.dpc.testing.KeyType;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolationException;
import java.io.IOException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PublicKeyDAOUnitTest extends AbstractDAOTest<PublicKeyEntity> {
    PublicKeyDAO publicKeyDAO;

    @BeforeEach
    public void setUp() {
        publicKeyDAO = new PublicKeyDAO(new DPCAuthManagedSessionFactory(db.getSessionFactory()));
    }

    @Test
    void writesPublicKey() throws IOException, NoSuchAlgorithmException {
        UUID orgId = UUID.randomUUID();
        PublicKeyEntity publicKeyEntity = createPublicKeyEntity(orgId, "test label");
        PublicKeyEntity returnedKey = db.inTransaction(() -> publicKeyDAO.persistPublicKey(publicKeyEntity));

        assertEquals(orgId, returnedKey.getOrganization_id());
        assertEquals("test label", returnedKey.getLabel());
        assertFalse(returnedKey.getCreatedAt().toString().isEmpty());
        assertFalse(returnedKey.getId().toString().isEmpty());
        assertNotNull(returnedKey.getPublicKey());
    }

    @Test
    void failsWritingPublicKeyWithTooLongLabel() throws IOException, NoSuchAlgorithmException {
        UUID orgId = UUID.randomUUID();
        PublicKeyEntity publicKeyEntity = createPublicKeyEntity(orgId, "test label".repeat(3));
        assertThrows(ConstraintViolationException.class, () ->
                db.inTransaction(() -> publicKeyDAO.persistPublicKey(publicKeyEntity)));

    }

    @Test
    void fetchesPublicKey() {
        UUID orgId1 = UUID.randomUUID();
        UUID orgId2 = UUID.randomUUID();

        db.inTransaction(() -> {
            try {
                publicKeyDAO.persistPublicKey(createPublicKeyEntity(orgId1, "label 1"));
                publicKeyDAO.persistPublicKey(createPublicKeyEntity(orgId1, "label 2"));
                publicKeyDAO.persistPublicKey(createPublicKeyEntity(orgId2, "label 3"));
            } catch (IOException | NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        });

        List<PublicKeyEntity> results = db.inTransaction(() -> publicKeyDAO.fetchPublicKeys(orgId1));

        assertEquals(2, results.size());

        PublicKeyEntity key1 = results.get(0);
        assertEquals(orgId1, key1.getOrganization_id());
        assertEquals("label 1", key1.getLabel());
        assertNotNull(key1.getPublicKey());

        PublicKeyEntity key2 = results.get(1);
        assertEquals(orgId1, key2.getOrganization_id());
        assertEquals("label 2", key2.getLabel());
        assertNotNull(key2.getPublicKey());
    }

    @Test
    void deletesPublicKey() {
        UUID orgId = UUID.randomUUID();

        PublicKeyEntity persistedPublicKey = db.inTransaction(() ->
            publicKeyDAO.persistPublicKey(createPublicKeyEntity(orgId, "label 1")));

        List<PublicKeyEntity> results = db.inTransaction(() -> {
            publicKeyDAO.deletePublicKey(persistedPublicKey);
            return publicKeyDAO.fetchPublicKeys(orgId);
        });

        assertEquals(0, results.size());
    }

    private PublicKeyEntity createPublicKeyEntity(UUID orgId, String label) throws IOException, NoSuchAlgorithmException {
        PublicKeyEntity newEntity = new PublicKeyEntity();
        newEntity.setId(UUID.randomUUID());
        newEntity.setOrganization_id(orgId);
        newEntity.setLabel(label);

        SubjectPublicKeyInfo mockInfo = mock(SubjectPublicKeyInfo.class);
        when(mockInfo.getEncoded()).thenReturn(generatePublicKeyBytes());
        newEntity.setPublicKey(mockInfo);
        return newEntity;
    }

    private byte[] generatePublicKeyBytes() throws NoSuchAlgorithmException {
        final KeyPair keyPair = APIAuthHelpers.generateKeyPair(KeyType.RSA);

        return keyPair.getPublic().getEncoded();
    }
}
