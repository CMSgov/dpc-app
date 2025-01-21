package gov.cms.dpc.macaroons.store;

/**
 * Simple wrapper class for returning both a Macaroon ID and Root Key
 */
public class IDKeyPair {

    private final String id;
    private final String key;

    public IDKeyPair(String id, String key) {
        this.id = id;
        this.key = key;
    }

    public String getId() {
        return id;
    }

    public String getKey() {
        return key;
    }
}
