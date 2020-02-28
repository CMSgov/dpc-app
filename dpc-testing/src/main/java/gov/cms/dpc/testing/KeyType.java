package gov.cms.dpc.testing;

/**
 * {@link java.security.KeyPair} type to generate for testing purposes
 */
public enum KeyType {
    RSA ("RSA", 4096),
    ECC ("EC", 256);

    private final String name;
    private final int keySize;

    KeyType(String name, int size) {
        this.name = name;
        this.keySize = size;
    }

    public String getName() {
        return name;
    }

    public int getKeySize() {
        return keySize;
    }
}
