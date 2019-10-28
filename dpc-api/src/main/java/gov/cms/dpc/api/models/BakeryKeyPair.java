package gov.cms.dpc.api.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import gov.cms.dpc.api.converters.KeyBytesToMimeConverter;
import gov.cms.dpc.api.converters.MimeToKeyBytesConverter;

import java.io.Serializable;

public class BakeryKeyPair implements Serializable {

    public static final long serialVersionUID = 42L;

    @JsonProperty(value = "public_key")
    @JsonSerialize(converter = KeyBytesToMimeConverter.class)
    @JsonDeserialize(converter = MimeToKeyBytesConverter.class)
    private byte[] publicKey;

    @JsonProperty(value = "private_key")
    @JsonSerialize(converter = KeyBytesToMimeConverter.class)
    @JsonDeserialize(converter = MimeToKeyBytesConverter.class)
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
}
