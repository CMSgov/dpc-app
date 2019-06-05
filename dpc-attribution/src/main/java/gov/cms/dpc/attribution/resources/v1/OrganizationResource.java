package gov.cms.dpc.attribution.resources.v1;

import gov.cms.dpc.attribution.jdbi.OrganizationDAO;
import gov.cms.dpc.attribution.resources.AbstractOrganizationResource;
import io.dropwizard.hibernate.UnitOfWork;
import org.hl7.fhir.dstu3.model.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

public class OrganizationResource extends AbstractOrganizationResource {

    private static final Logger logger = LoggerFactory.getLogger(OrganizationResource.class);

    private final OrganizationDAO dao;

    @Inject
    OrganizationResource(OrganizationDAO dao) {
        this.dao = dao;
    }

    @Override
    @UnitOfWork
    public Response createOrganization(Organization organization) {

        try {
            this.dao.registerOrganization(organization);
            return Response.ok().build();
        } catch (Exception e) {
            logger.error("Error: ", e);
            throw e;
        }

    }
}
