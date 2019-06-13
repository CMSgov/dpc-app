package gov.cms.dpc.macaroons.store;

import java.security.SecureRandom;

class MemoryStoreTest extends AbstractStoreTest {

    MemoryStoreTest() {
        super(new MemoryRootKeyStore(new SecureRandom()));
    }

    @Override
    protected void teardown() {
        // Not used
    }
}
