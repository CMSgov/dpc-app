package gov.cms.dpc.fhir.dropwizard.handlers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import gov.cms.dpc.fhir.annotations.FHIR;
import org.hl7.fhir.dstu3.model.BaseResource;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Provider
@FHIR
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
    public BaseResource readFrom(Class<BaseResource> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        final IParser parser = ctx.newJsonParser();
        return (BaseResource) parser.parseResource(new InputStreamReader(entityStream));
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
        parser.encodeResourceToWriter(baseResource, new OutputStreamWriter(entityStream));
    }
}
