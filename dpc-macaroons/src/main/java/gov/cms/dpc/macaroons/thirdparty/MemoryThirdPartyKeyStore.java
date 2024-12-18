package gov.cms.dpc.macaroons.thirdparty;

import jakarta.inject.Singleton;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory third-party key store that holds keys in a {@link ConcurrentHashMap} without any expiration policy
 */
@Singleton
public class MemoryThirdPartyKeyStore implements IThirdPartyKeyStore {

    private final Map<String, byte[]> store;

    public MemoryThirdPartyKeyStore() {
        this.store = new ConcurrentHashMap<>();
    }

    @Override
    public Optional<byte[]> getPublicKey(String location) {
        return Optional.ofNullable(this.store.get(location));
    }

    @Override
    public void setPublicKey(String location, byte[] key) {
        this.store.put(location, key);
    }
}
