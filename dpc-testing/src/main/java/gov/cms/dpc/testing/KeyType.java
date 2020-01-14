package gov.cms.dpc.testing;

/**
 * {@link java.security.KeyPair} type to generate for testing purposes
 */
public enum KeyType {
    RSA ("RSA", 4096),
    ECC ("EC", 256);

    private final String keyType;
    private final int keySize;

    KeyType(String keyType, int keySize) {
        this.keyType = keyType;
        this.keySize = keySize;
    }

    public String getKeyType() {
        return keyType;
    }

    public int getKeySize() {
        return keySize;
    }
}
