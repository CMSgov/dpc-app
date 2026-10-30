package gov.cms.dpc.fhir.dropwizard.handlers.exceptions;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
// TODO: Delete this class when we remove stu3
public class DefaultFHIRExceptionMapper extends AbstractVersionedExceptionMapper<Throwable> {

    @Inject
    public DefaultFHIRExceptionMapper(@Context ResourceInfo info, @Context UriInfo uriInfo) {
        super(info, uriInfo);
    }

    @Override
    protected ExceptionMapper<Throwable> createStu3Handler(ResourceInfo info) {
        return new gov.cms.dpc.fhir.dropwizard.handlers.exceptions.stu3.DefaultFHIRExceptionHandler(info);
    }

    @Override
    protected ExceptionMapper<Throwable> createR4Handler(ResourceInfo info) {
        return new gov.cms.dpc.fhir.dropwizard.handlers.exceptions.r4.DefaultFHIRExceptionHandler(info);
    }
}
