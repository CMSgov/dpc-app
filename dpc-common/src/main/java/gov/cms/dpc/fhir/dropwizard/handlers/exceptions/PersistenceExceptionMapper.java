package gov.cms.dpc.fhir.dropwizard.handlers.exceptions;

import jakarta.persistence.PersistenceException;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
// TODO: Delete this class when we remove stu3
public class PersistenceExceptionMapper extends AbstractVersionedExceptionMapper<PersistenceException> {

    @Inject
    public PersistenceExceptionMapper(@Context ResourceInfo info, @Context UriInfo uriInfo) {
        super(info, uriInfo);
    }

    @Override
    protected ExceptionMapper<PersistenceException> createStu3Handler(ResourceInfo info) {
        return new gov.cms.dpc.fhir.dropwizard.handlers.exceptions.stu3.PersistenceExceptionHandler(info);
    }

    @Override
    protected ExceptionMapper<PersistenceException> createR4Handler(ResourceInfo info) {
        return new gov.cms.dpc.fhir.dropwizard.handlers.exceptions.r4.PersistenceExceptionHandler(info);
    }
}
