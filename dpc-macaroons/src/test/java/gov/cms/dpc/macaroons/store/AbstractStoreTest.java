package gov.cms.dpc.macaroons.store;

import gov.cms.dpc.macaroons.exceptions.BakeryException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

abstract class AbstractStoreTest {

    private final IRootKeyStore store;

    AbstractStoreTest(IRootKeyStore store) {
        this.store = store;
    }

    @Test
    void simpleCreateTest() {
        final String key = store.create();
        final String key2 = store.get("0");
        assertEquals(key, key2, "Keys should be equal");
    }

    @Test
    void testInvalidID() {
        assertThrows(BakeryException.class, () -> store.get("1"), "Should throw an exception on unknown ID");
    }


}
