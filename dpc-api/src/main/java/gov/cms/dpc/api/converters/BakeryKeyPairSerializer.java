package gov.cms.dpc.api.converters;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import gov.cms.dpc.macaroons.thirdparty.BakeryKeyPair;

import java.io.IOException;
import java.io.Serial;

public class BakeryKeyPairSerializer extends StdSerializer<BakeryKeyPair> {

    @Serial
    private static final long serialVersionUID = 42L;

    BakeryKeyPairSerializer() {
        this(null);
    }

    private BakeryKeyPairSerializer(Class<BakeryKeyPair> t) {
        super(t);
    }

    @Override
    public void serialize(BakeryKeyPair value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeBinaryField("public_key", value.getPublicKey());
        gen.writeBinaryField("private_key", value.getPrivateKey());
        gen.writeEndObject();
    }
}
