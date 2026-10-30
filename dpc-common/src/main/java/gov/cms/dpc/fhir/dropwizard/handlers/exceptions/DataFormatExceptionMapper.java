package gov.cms.dpc.fhir.dropwizard.handlers.exceptions;

import ca.uhn.fhir.parser.DataFormatException;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
// TODO: Delete this class when we remove stu3
public class DataFormatExceptionMapper extends AbstractVersionedExceptionMapper<DataFormatException> {

    @Inject
    public DataFormatExceptionMapper(@Context ResourceInfo info, @Context UriInfo uriInfo) {
        super(info, uriInfo);
    }

    @Override
    protected ExceptionMapper<DataFormatException> createStu3Handler(ResourceInfo info) {
        return new gov.cms.dpc.fhir.dropwizard.handlers.exceptions.stu3.DataFormatExceptionHandler(info);
    }

    @Override
    protected ExceptionMapper<DataFormatException> createR4Handler(ResourceInfo info) {
        return new gov.cms.dpc.fhir.dropwizard.handlers.exceptions.r4.DataFormatExceptionHandler(info);
    }
}
