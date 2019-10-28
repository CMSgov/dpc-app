package gov.cms.dpc.api.converters;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import gov.cms.dpc.macaroons.thirdparty.BakeryKeyPair;

import java.io.IOException;
import java.util.Base64;

public class BakeryKeyPairSerializer extends StdSerializer<BakeryKeyPair> {

    public static final long serialVersionUID = 42L;

    private final Base64.Encoder encoder;

    BakeryKeyPairSerializer() {
        this(null);
    }

    private BakeryKeyPairSerializer(Class<BakeryKeyPair> t) {
        super(t);
        this.encoder = Base64.getMimeEncoder();
    }

    @Override
    public void serialize(BakeryKeyPair value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        final byte[] publicKey = this.encoder.encode(value.getPublicKey());
        final byte[] privateKey = this.encoder.encode(value.getPrivateKey());
        gen.writeStartObject();
        gen.writeBinaryField("public_key", publicKey);
        gen.writeBinaryField("private_key", privateKey);
        gen.writeEndObject();
    }
}
