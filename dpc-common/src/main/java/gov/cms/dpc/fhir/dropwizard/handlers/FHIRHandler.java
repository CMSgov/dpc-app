package gov.cms.dpc.fhir.dropwizard.handlers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import gov.cms.dpc.fhir.FHIRMediaTypes;
import gov.cms.dpc.fhir.annotations.FHIR;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.BaseResource;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

@Provider
@FHIR
@Consumes({FHIRMediaTypes.FHIR_JSON})
@Produces({FHIRMediaTypes.FHIR_JSON})
public class FHIRHandler implements MessageBodyReader<BaseResource>, MessageBodyWriter<BaseResource> {

    private final FhirContext ctx;

    @Inject
    public FHIRHandler(FhirContext context) {
        this.ctx = context;
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return BaseResource.class.isAssignableFrom(type);
    }

    @Override
    public BaseResource readFrom(Class<BaseResource> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws WebApplicationException {
        final IParser parser = ctx.newJsonParser();
        try {
            return (BaseResource) parser.parseResource(new InputStreamReader(entityStream, StandardCharsets.UTF_8));
            // We need to manually handle the DataFormatException because our custom exception handlers aren't loaded yet.
        } catch (DataFormatException e) {
            String message = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            // Bad request if not parsable; unprocessable if parsable but in violation of profile or business rules
            int status = message.contains("Invalid attribute value") ? HttpStatus.UNPROCESSABLE_ENTITY_422 : HttpStatus.BAD_REQUEST_400;
            throw new WebApplicationException(message, status);
        }
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return BaseResource.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(BaseResource baseResource, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        // Deprecated and ignored by Jersey
        return 0;
    }

    @Override
    public void writeTo(BaseResource baseResource, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        final IParser parser = ctx.newJsonParser();
        parser.encodeResourceToWriter(baseResource, new OutputStreamWriter(entityStream, StandardCharsets.UTF_8));
    }
}
