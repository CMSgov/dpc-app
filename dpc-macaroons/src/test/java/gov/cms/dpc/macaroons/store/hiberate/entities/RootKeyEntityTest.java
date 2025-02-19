package gov.cms.dpc.macaroons.store.hiberate.entities;

import gov.cms.dpc.macaroons.store.hibernate.entities.RootKeyEntity;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RootKeyEntityTest {

    @Test
    void gettersAndSetters() {
        RootKeyEntity rootKey = new RootKeyEntity();
        String id = "root key id";
        String rootKeyString = "root key string";
        OffsetDateTime createdAt = OffsetDateTime.now();
        OffsetDateTime expiresAt = OffsetDateTime.now();

        rootKey.setId(id);
        rootKey.setRootKey(rootKeyString);
        rootKey.setCreated(createdAt);
        rootKey.setExpires(expiresAt);

        assertEquals(id, rootKey.getId());
        assertEquals(rootKeyString, rootKey.getRootKey());
        assertEquals(createdAt, rootKey.getCreated());
        assertEquals(expiresAt, rootKey.getExpires());
    }
}
