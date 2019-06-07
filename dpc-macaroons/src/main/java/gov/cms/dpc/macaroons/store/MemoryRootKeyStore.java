package gov.cms.dpc.macaroons.store;

import gov.cms.dpc.macaroons.exceptions.BakeryException;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.security.SecureRandom;

/**
 * Simple wrapper class around a fixed {@link String} root key.
 * Incredibly
 */
@Singleton
public class MemoryRootKeyStore implements IRootKeyStore {

    private final String rootKey;

    @Inject
    public MemoryRootKeyStore(SecureRandom random) {
        final byte[] keyBytes = new byte[24];
        random.nextBytes(keyBytes);
        this.rootKey = new String(keyBytes);
    }

    @Override
    public String create() {
        return rootKey;
    }

    @Override
    public String get(String macaroonID) {
        if (macaroonID.equals("0")) {
            return rootKey;
        }

        throw new BakeryException(String.format("Cannot find root key for ID: %s", macaroonID));
    }
}
