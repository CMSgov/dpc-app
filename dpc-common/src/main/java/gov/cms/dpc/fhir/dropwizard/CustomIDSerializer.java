package gov.cms.dpc.fhir.dropwizard;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.hl7.fhir.dstu3.model.IdType;

import java.io.IOException;

public class CustomIDSerializer extends StdSerializer<IdType> {

    private final FhirContext context;

    public CustomIDSerializer(FhirContext ctx) {
        this(null, ctx);
    }

    protected CustomIDSerializer(Class<IdType> t, FhirContext ctx) {
        super(t);
        this.context = ctx;
    }


    @Override
    public void serialize(IdType value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeString(value.getValue());
    }
}
