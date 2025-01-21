package gov.cms.dpc.macaroons.store;

/**
 * Simple wrapper class for returning both a Macaroon ID and Root Key
 */
public record IDKeyPair(String id, String key) {
    public String getId() {
        return id;
    }

    public String getKey() {
        return key;
    }
}
