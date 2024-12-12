package gov.cms.dpc.macaroons.store;

import gov.cms.dpc.macaroons.exceptions.BakeryException;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(BufferedLoggerHandler.class)
@DisplayName("Macaroon store tests")
public abstract class AbstractStoreTest {

    protected final IRootKeyStore store;

    public AbstractStoreTest(IRootKeyStore store) {
        this.store = store;
    }

    @AfterAll
    void shutdown() {
        this.teardown();
    }

    @Test

    @DisplayName("Create key pair 🥳")
    void simpleCreateTest() {
        final IDKeyPair idKeyPair = store.create();
        final String key2 = store.get(idKeyPair.getId());
        assertEquals(idKeyPair.getKey(), key2, "Keys should be equal");
    }

    @Test
    @DisplayName("Get key for unknown ID 🤮")
    void testInvalidID() {
        assertThrows(BakeryException.class, () -> store.get("1"), "Should throw an exception on unknown ID");
    }

    protected abstract void teardown();


}
