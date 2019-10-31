package gov.cms.dpc.api.converters;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import gov.cms.dpc.macaroons.thirdparty.BakeryKeyPair;

import java.io.IOException;
import java.util.Base64;

public class BakeryKeyPairDeserializer extends StdDeserializer<BakeryKeyPair> {

    public static final long serialVersionUID = 42L;

    private final Base64.Decoder decoder;

    BakeryKeyPairDeserializer() {
        this(null);
    }

    private BakeryKeyPairDeserializer(Class<?> vc) {
        super(vc);
        this.decoder = Base64.getMimeDecoder();
    }

    @Override
    public BakeryKeyPair deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        final JsonNode node = p.getCodec().readTree(p);

        final JsonNode publicKeyNode = node.get("public_key");
        if (publicKeyNode == null) {
            throw new IllegalArgumentException("Keypair must have public key value");
        }
        final JsonNode privateKeyNode = node.get("private_key");
        if (privateKeyNode == null) {
            throw new IllegalArgumentException("Keypair must have private key value");
        }
        final byte[] publicKey = this.decoder.decode(publicKeyNode.asText());
        final byte[] privateKey = this.decoder.decode(privateKeyNode.asText());

        return new BakeryKeyPair(publicKey, privateKey);
    }
}
