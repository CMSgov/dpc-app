package gov.cms.dpc.macaroons.store;

import gov.cms.dpc.macaroons.exceptions.BakeryException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public abstract class AbstractStoreTest {

    private final IRootKeyStore store;

    public AbstractStoreTest(IRootKeyStore store) {
        this.store = store;
    }

    @Test
    void simpleCreateTest() {
        final IDKeyPair idKeyPair = store.create();
        final String key2 = store.get(idKeyPair.getId());
        assertEquals(idKeyPair.getKey(), key2, "Keys should be equal");
    }

    @Test
    void testInvalidID() {
        assertThrows(BakeryException.class, () -> store.get("1"), "Should throw an exception on unknown ID");
    }


}
