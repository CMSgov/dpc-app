package gov.cms.dpc.macaroons.store;

/**
 * Interface for managing Macaroon root keys
 */
public interface IRootKeyStore {

    /**
     * Create a new ID/RootKey pair and persist the root key
     * @return - {@link IDKeyPair}
     */
    IDKeyPair create();

    /**
     * Get the RootKey associated to the given Macaroon ID
     * @param macaroonID - {@link String} macaroon ID to get key for
     * @return - {@link String} Root Key for Macaroon
     * @throws gov.cms.dpc.macaroons.exceptions.BakeryException if no key exists
     */
    String get(String macaroonID);
}
