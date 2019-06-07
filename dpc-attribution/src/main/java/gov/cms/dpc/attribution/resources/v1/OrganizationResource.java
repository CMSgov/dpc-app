package gov.cms.dpc.attribution.resources.v1;

import com.github.nitram509.jmacaroons.Macaroon;
import gov.cms.dpc.attribution.jdbi.OrganizationDAO;
import gov.cms.dpc.attribution.resources.AbstractOrganizationResource;
import gov.cms.dpc.common.entities.EndpointEntity;
import gov.cms.dpc.fhir.converters.EndpointConverter;
import gov.cms.dpc.macaroons.MacaroonCaveat;
import gov.cms.dpc.macaroons.MacaroonsBakery;
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
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
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
    @Path("/{organizationID}/token")
    @Override
    public Response getOrganizationToken(@PathParam("organizationID") String organizationID, @QueryParam("refresh") BooleanParam refresh) {

        // Create some caveats
        final List<MacaroonCaveat> caveats = List.of(
                new MacaroonCaveat("organization_id", MacaroonCaveat.Operator.EQ, organizationID)
        );

        final Macaroon macaroon = this.bakery.createMacaroon(caveats);

        return Response.ok().entity(this.bakery.serializeMacaroon(macaroon)).build();
    }
}
