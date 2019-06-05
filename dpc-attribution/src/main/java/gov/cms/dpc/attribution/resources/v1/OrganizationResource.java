package gov.cms.dpc.attribution.resources.v1;

import gov.cms.dpc.attribution.jdbi.OrganizationDAO;
import gov.cms.dpc.attribution.resources.AbstractOrganizationResource;
import gov.cms.dpc.common.entities.EndpointEntity;
import gov.cms.dpc.fhir.converters.EndpointConverter;
import io.dropwizard.hibernate.UnitOfWork;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class OrganizationResource extends AbstractOrganizationResource {

    private static final Logger logger = LoggerFactory.getLogger(OrganizationResource.class);
    private final OrganizationDAO dao;

    @Inject
    OrganizationResource(OrganizationDAO dao) {
        this.dao = dao;
    }

    @Override
    @UnitOfWork
    public Response createOrganization(Bundle transactionBundle) {

        final Optional<Organization> organization = transactionBundle
                .getEntry()
                .stream()
                .filter(entry -> entry.hasResource() && entry.getResource().getResourceType() == ResourceType.Organization)
                .map(entry -> (Organization) entry.getResource())
                .findFirst();

        if (organization.isEmpty()) {
            return Response.status(HttpStatus.UNPROCESSABLE_ENTITY_422).entity("Must provide organization to register").build();
        }

        // Build the endpoints
        final List<EndpointEntity> endpoints = transactionBundle
                .getEntry()
                .stream()
                .filter(entry -> entry.hasResource() && entry.getResource().getResourceType() == ResourceType.Endpoint)
                .map(entry -> (Endpoint) entry.getResource())
                .map(EndpointConverter::convert)
                .collect(Collectors.toList());

        try {
            this.dao.registerOrganization(organization.get(), endpoints);
            return Response.ok().build();
        } catch (Exception e) {
            logger.error("Error: ", e);
            throw e;
        }
    }
}
