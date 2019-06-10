package gov.cms.dpc.attribution.resources.v1;

import com.github.nitram509.jmacaroons.Macaroon;
import gov.cms.dpc.attribution.jdbi.OrganizationDAO;
import gov.cms.dpc.attribution.resources.AbstractOrganizationResource;
import gov.cms.dpc.common.entities.EndpointEntity;
import gov.cms.dpc.common.entities.OrganizationEntity;
import gov.cms.dpc.fhir.converters.EndpointConverter;
import gov.cms.dpc.macaroons.MacaroonCaveat;
import gov.cms.dpc.macaroons.MacaroonsBakery;
import gov.cms.dpc.macaroons.exceptions.BakeryException;
import io.dropwizard.hibernate.UnitOfWork;
import io.dropwizard.jersey.params.BooleanParam;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class OrganizationResource extends AbstractOrganizationResource {

    private static final Logger logger = LoggerFactory.getLogger(OrganizationResource.class);
    private final OrganizationDAO dao;
    private final MacaroonsBakery bakery;

    @Inject
    OrganizationResource(OrganizationDAO dao, MacaroonsBakery bakery) {
        this.dao = dao;
        this.bakery = bakery;
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

    @GET
    @Path("/{organizationID}/token/create")
    @UnitOfWork
    @Override
    public String getOrganizationToken(@PathParam("organizationID") UUID organizationID, @QueryParam("refresh") BooleanParam refresh) {
        final Optional<OrganizationEntity> entityOptional = this.dao.fetchOrganization(organizationID);

        final OrganizationEntity entity = entityOptional.orElseThrow(() -> new WebApplicationException(String.format("Cannot find Organization: %s", organizationID), Response.Status.NOT_FOUND));
        // Create some caveats
        final List<MacaroonCaveat> caveats = List.of(
                new MacaroonCaveat("organization_id", MacaroonCaveat.Operator.EQ, organizationID.toString())
        );
        final Macaroon macaroon = this.bakery.createMacaroon(caveats);

        // Add the macaroon ID to the organization and update it
        entity.setTokenIDs(Collections.singletonList(macaroon.identifier));
        this.dao.updateOrganization(entity);

        // Return the
        return new String(this.bakery.serializeMacaroon(macaroon, true));
    }

    @GET
    @Path("/{organizationID}/token/verify")
    public boolean verifyOrganizationToken(@PathParam("organizationID") UUID organizationID, @QueryParam("token") String token) {
        final Macaroon macaroon = this.bakery.deserializeMacaroon(token);
        try {
            this.bakery.verifyMacaroon(macaroon);
        } catch (BakeryException e) {
            logger.error("Macaroon verification failed.", e);
            return false;
        }

        return true;
    }
}
