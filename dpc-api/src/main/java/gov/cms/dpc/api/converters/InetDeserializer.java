package gov.cms.dpc.api.converters;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.hypersistence.utils.hibernate.type.basic.Inet;

import java.io.IOException;

/**
 * Custom deserializer for the {@link Inet} class.  Jackson can serialize it fine on its own, but needs a little
 * help in the other direction.
 */
public class InetDeserializer extends StdDeserializer<Inet> {
    public InetDeserializer() {this(null);}
    private InetDeserializer(Class<?> vc) {super(vc);}

    @Override
    public Inet deserialize(JsonParser p, DeserializationContext deserializationContext) throws IOException {
        JsonNode node = p.getCodec().readTree(p);

        JsonNode addressNode = node.get("address");
        if (addressNode == null) {
            throw new IllegalArgumentException("Inet type must have address value");
        }

        return new Inet(addressNode.asText());
    }
}
