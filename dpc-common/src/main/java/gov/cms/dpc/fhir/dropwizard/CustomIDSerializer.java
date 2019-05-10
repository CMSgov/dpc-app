package gov.cms.dpc.fhir.dropwizard;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.hl7.fhir.dstu3.model.IdType;

import java.io.IOException;

public class CustomIDSerializer extends StdSerializer<IdType> {

    public static final long serialVersionUID = 42L;

    public CustomIDSerializer() {
        this(null);
    }

    protected CustomIDSerializer(Class<IdType> t) {
        super(t);
    }


    @Override
    public void serialize(IdType value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeString(value.getValue());
    }
}
