package gov.cms.dpc.api.entities;

import gov.cms.dpc.api.entities.PublicKeyEntity;
import gov.cms.dpc.testing.APIAuthHelpers;
import gov.cms.dpc.testing.KeyType;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PublicKeyEntityTest {

    @Test
    void testGettersAndSetters() throws IOException, NoSuchAlgorithmException {
        PublicKeyEntity publicKey = new PublicKeyEntity();
        UUID id = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        SubjectPublicKeyInfo mockInfo = mock(SubjectPublicKeyInfo.class);
        when(mockInfo.getEncoded()).thenReturn(generatePublicKeyBytes());
        String label = "label string";
        OffsetDateTime createdAt = OffsetDateTime.now();

        publicKey.setId(id);
        publicKey.setOrganization_id(orgId);
        publicKey.setPublicKey(mockInfo);
        publicKey.setLabel(label);
        publicKey.setCreatedAt(createdAt);

        assertEquals(id, publicKey.getId());
        assertEquals(orgId, publicKey.getOrganization_id());
        assertEquals(mockInfo, publicKey.getPublicKey());
        assertEquals(label, publicKey.getLabel());
        assertEquals(createdAt, publicKey.getCreatedAt());
    }

    private byte[] generatePublicKeyBytes() throws NoSuchAlgorithmException {
        final KeyPair keyPair = APIAuthHelpers.generateKeyPair(KeyType.RSA);

        return keyPair.getPublic().getEncoded();
    }
}
