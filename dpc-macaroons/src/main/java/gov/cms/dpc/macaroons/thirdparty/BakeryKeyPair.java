package gov.cms.dpc.macaroons.thirdparty;

import org.whispersystems.curve25519.Curve25519;
import org.whispersystems.curve25519.Curve25519KeyPair;

import java.io.Serializable;

public class BakeryKeyPair implements Serializable {

    private static final long serialVersionUID = 42L;

    private byte[] publicKey;

    private byte[] privateKey;

    public BakeryKeyPair() {
        // Jackson required
    }

    public BakeryKeyPair(byte[] publicKey, byte[] privateKey) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(byte[] publicKey) {
        this.publicKey = publicKey;
    }

    public byte[] getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(byte[] privateKey) {
        this.privateKey = privateKey;
    }

    public static BakeryKeyPair generate() {
        final Curve25519KeyPair keyPair = Curve25519.getInstance(Curve25519.BEST).generateKeyPair();
        return new BakeryKeyPair(keyPair.getPublicKey(), keyPair.getPrivateKey());
    }
}
