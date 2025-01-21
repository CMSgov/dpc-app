package gov.cms.dpc.api.converters;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import gov.cms.dpc.macaroons.thirdparty.BakeryKeyPair;

import java.io.IOException;

public class BakeryKeyPairDeserializer extends StdDeserializer<BakeryKeyPair> {
    private static final long serialVersionUID = 42L;

    BakeryKeyPairDeserializer() {
        this(null);
    }

    private BakeryKeyPairDeserializer(Class<?> vc) {
        super(vc);
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

        return new BakeryKeyPair(publicKeyNode.binaryValue(), publicKeyNode.binaryValue());
    }
}
