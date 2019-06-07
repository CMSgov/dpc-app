package gov.cms.dpc.macaroons.store;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MemoryStoreTest extends AbstractStoreTest {

    MemoryStoreTest() {
        super(new MemoryRootKeyStore(new SecureRandom()));
    }
}
