package gov.cms.dpc.fhir.dropwizard.handlers.exceptions;

import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
// TODO: Delete this class when we remove stu3
public class HAPIExceptionMapper extends AbstractVersionedExceptionMapper<BaseServerResponseException> {

    @Inject
    public HAPIExceptionMapper(@Context ResourceInfo info, @Context UriInfo uriInfo) {
        super(info, uriInfo);
    }

    @Override
    protected ExceptionMapper<BaseServerResponseException> createStu3Handler(ResourceInfo info) {
        return new gov.cms.dpc.fhir.dropwizard.handlers.exceptions.stu3.HAPIExceptionHandler();
    }

    @Override
    protected ExceptionMapper<BaseServerResponseException> createR4Handler(ResourceInfo info) {
        return new gov.cms.dpc.fhir.dropwizard.handlers.exceptions.r4.HAPIExceptionHandler();
    }
}
