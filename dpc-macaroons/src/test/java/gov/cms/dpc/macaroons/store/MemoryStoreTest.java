package gov.cms.dpc.macaroons.store;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

class MemoryStoreTest extends AbstractStoreTest {

    MemoryStoreTest() {
        super(new MemoryRootKeyStore(new SecureRandom()));
    }

    @Test
    void testKeyGeneratedCorrectly() {
        assertNotEquals(0, this.store.get("0").hashCode(), "Should have random root key");
    }

    @Override
    protected void teardown() {
        // Not used
    }
}
