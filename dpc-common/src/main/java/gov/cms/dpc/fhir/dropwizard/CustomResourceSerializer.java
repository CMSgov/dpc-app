package gov.cms.dpc.fhir.dropwizard;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.hl7.fhir.dstu3.model.Resource;

import java.io.IOException;

public class CustomResourceSerializer extends StdSerializer<Resource> {

    private final FhirContext context;

    public CustomResourceSerializer(FhirContext ctx) {
        this(null, ctx);
    }

    private CustomResourceSerializer(Class<Resource> clazz, FhirContext ctx) {
        super(clazz);
        this.context = ctx;

    }

    @Override
    public void serialize(Resource value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        final String encoded = context.newJsonParser().encodeResourceToString(value);
        gen.writeRawValue(encoded);
    }
}
