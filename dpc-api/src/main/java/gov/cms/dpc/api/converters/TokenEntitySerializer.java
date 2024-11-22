package gov.cms.dpc.api.converters;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import gov.cms.dpc.api.entities.TokenEntity;
import gov.cms.dpc.fhir.FHIRFormatters;

import java.io.IOException;

public class TokenEntitySerializer extends StdSerializer<TokenEntity> {
    public static final long serialVersionUID = 42L;

    public TokenEntitySerializer() {
        super(TokenEntity.class);
    }

    @Override
    public void serialize(TokenEntity tokenEntity, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("id", tokenEntity.getId());
        jsonGenerator.writeStringField("organizationID", tokenEntity.getOrganizationID().toString());
        jsonGenerator.writeStringField("tokenType", tokenEntity.getTokenType().name());
        jsonGenerator.writeStringField("label", tokenEntity.getLabel());
        if(tokenEntity.getCreatedAt() != null)
            jsonGenerator.writeStringField("createdAt", FHIRFormatters.INSTANT_FORMATTER.format(tokenEntity.getCreatedAt()));
        if(tokenEntity.getExpiresAt() != null)
            jsonGenerator.writeStringField("expiresAt", FHIRFormatters.INSTANT_FORMATTER.format(tokenEntity.getExpiresAt()));
        if(tokenEntity.getToken() != null && !tokenEntity.getToken().isBlank())
            jsonGenerator.writeStringField("token", tokenEntity.getToken());  // Add explicit logging here.
        jsonGenerator.writeEndObject();
    }
}
