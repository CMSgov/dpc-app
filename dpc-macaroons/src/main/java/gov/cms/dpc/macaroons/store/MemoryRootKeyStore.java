package gov.cms.dpc.macaroons.store;

import gov.cms.dpc.macaroons.exceptions.BakeryException;
import gov.cms.dpc.macaroons.helpers.SecretHelpers;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * Simple wrapper class around a fixed {@link String} root key.
 * Incredibly simplistic, only good for testing
 */
@Singleton
public class MemoryRootKeyStore implements IRootKeyStore {

    private final String rootKey;
    private final SecureRandom random;

    @Inject
    public MemoryRootKeyStore(SecureRandom random) {
        this.random = random;
        final byte[] keyBytes = new byte[24];
        random.nextBytes(keyBytes);
        this.rootKey = new String(keyBytes, StandardCharsets.UTF_8);
    }

    @Override
    public IDKeyPair create() {
        return new IDKeyPair("0", rootKey);
    }

    @Override
    public String get(String macaroonID) {
        if (macaroonID.equals("0")) {
            return rootKey;
        }

        throw new BakeryException(String.format("Cannot find root key for ID: %s", macaroonID));
    }

    @Override
    public String generateKey() {
        return SecretHelpers.generateSecretKey(this.random);
    }
}
