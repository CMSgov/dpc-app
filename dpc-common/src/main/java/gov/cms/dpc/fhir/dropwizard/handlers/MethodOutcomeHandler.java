package gov.cms.dpc.fhir.dropwizard.handlers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.dropwizard.CustomIDSerializer;
import gov.cms.dpc.fhir.dropwizard.CustomResourceSerializer;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Resource;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Provider
@FHIR
public class MethodOutcomeHandler implements MessageBodyWriter<MethodOutcome> {

    private final ObjectMapper mapper;

    @Inject
    MethodOutcomeHandler(FhirContext context) {
        final CustomResourceSerializer serializer = new CustomResourceSerializer(context);
        final CustomIDSerializer customIDSerializer = new CustomIDSerializer();
        final SimpleModule module = new SimpleModule();
        module.addSerializer(Resource.class, serializer);
        module.addSerializer(IdType.class, customIDSerializer);
        mapper = new ObjectMapper();
        mapper.registerModule(module);
    }


    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return MethodOutcome.class.isAssignableFrom(type);
    }

    @Override
    public void writeTo(MethodOutcome methodOutcome, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
//        mapper.write
        mapper.writeValue(entityStream, methodOutcome);
    }
}
