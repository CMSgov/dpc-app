package gov.cms.dpc.fhir.dropwizard.handlers.exceptions;

import io.dropwizard.jersey.validation.JerseyViolationException;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
// TODO: Delete this class when we remove stu3
public class JerseyExceptionMapper extends AbstractVersionedExceptionMapper<JerseyViolationException> {

    @Inject
    public JerseyExceptionMapper(@Context ResourceInfo info, @Context UriInfo uriInfo) {
        super(info, uriInfo);
    }

    @Override
    protected ExceptionMapper<JerseyViolationException> createStu3Handler(ResourceInfo info) {
        return new gov.cms.dpc.fhir.dropwizard.handlers.exceptions.stu3.JerseyExceptionHandler(info);
    }

    @Override
    protected ExceptionMapper<JerseyViolationException> createR4Handler(ResourceInfo info) {
        return new gov.cms.dpc.fhir.dropwizard.handlers.exceptions.r4.JerseyExceptionHandler(info);
    }
}
