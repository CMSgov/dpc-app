package gov.cms.dpc.attribution.resources.v1;

import com.github.nitram509.jmacaroons.Macaroon;
import gov.cms.dpc.attribution.jdbi.OrganizationDAO;
import gov.cms.dpc.attribution.resources.AbstractOrganizationResource;
import gov.cms.dpc.common.entities.EndpointEntity;
import gov.cms.dpc.common.entities.OrganizationEntity;
import gov.cms.dpc.common.entities.TokenEntity;
import gov.cms.dpc.fhir.converters.EndpointConverter;
import gov.cms.dpc.macaroons.MacaroonBakery;
import gov.cms.dpc.macaroons.MacaroonCaveat;
import gov.cms.dpc.macaroons.exceptions.BakeryException;
import io.dropwizard.hibernate.UnitOfWork;
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
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class OrganizationResource extends AbstractOrganizationResource {

    private static final Logger logger = LoggerFactory.getLogger(OrganizationResource.class);
    private final OrganizationDAO dao;
    private final MacaroonBakery bakery;

    @Inject
    OrganizationResource(OrganizationDAO dao, MacaroonBakery bakery) {
        this.dao = dao;
        this.bakery = bakery;
    }

    @Override
    @GET
    @UnitOfWork
    public Bundle searchAndValidateOrganizations(@QueryParam("_tag") String tokenTag) {
        if (tokenTag == null) {
            throw new WebApplicationException("Must have token to query", Response.Status.BAD_REQUEST);
        }

        final Macaroon macaroon = this.parseTokenTag(tokenTag);

        final List<OrganizationEntity> organizationEntities = this.dao.searchByToken(macaroon.identifier);

        if (organizationEntities.isEmpty()) {
            throw new WebApplicationException("Cannot find organization with registered token", Response.Status.NOT_FOUND);
        }

        // There should only ever be a single entity per token
        assert (organizationEntities.size() == 1);

        final OrganizationEntity organizationEntity = organizationEntities.get(0);

        // Validate the token
        if (!validateMacaroon(organizationEntity.getId(), macaroon)) {
            throw new WebApplicationException(String.format("Invalid token for organization %s", organizationEntity.getId().toString()), Response.Status.UNAUTHORIZED);
        }

        final Bundle bundle = new Bundle();
        bundle.addEntry().setResource(organizationEntity.toFHIR());

        return bundle;
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
    @Path("/{organizationID}")
    @UnitOfWork
    @Override
    public Organization getOrganization(@PathParam("organizationID") UUID organizationID) {
        final Optional<OrganizationEntity> orgOptional = this.dao.fetchOrganization(organizationID);
        final OrganizationEntity organizationEntity = orgOptional.orElseThrow(() -> new WebApplicationException(String.format("Cannot find organization '%s'", organizationID), Response.Status.NOT_FOUND));
        return organizationEntity.toFHIR();
    }

    @GET
    @Path("/{organizationID}/token")
    @UnitOfWork
    @Override
    public List<String> getOrganizationTokens(@PathParam("organizationID") UUID organizationID) {
        final Optional<OrganizationEntity> entityOptional = this.dao.fetchOrganization(organizationID);

        final OrganizationEntity entity = entityOptional.orElseThrow(() -> new WebApplicationException(String.format("Cannot find Organization: %s", organizationID), Response.Status.NOT_FOUND));

        return entity
                .getTokens()
                .stream()
                .map(TokenEntity::getId)
                .collect(Collectors.toList());
    }

    @POST
    @Path("/{organizationID}/token")
    @UnitOfWork
    @Override
    public String createOrganizationToken(@PathParam("organizationID") UUID organizationID) {
        final Optional<OrganizationEntity> entityOptional = this.dao.fetchOrganization(organizationID);

        final OrganizationEntity entity = entityOptional.orElseThrow(() -> new WebApplicationException(String.format("Cannot find Organization: %s", organizationID), Response.Status.NOT_FOUND));

        final Macaroon macaroon = generateMacaroon(organizationID);

        // Add the macaroon ID to the organization and update it
        final ArrayList<TokenEntity> tokens = new ArrayList<>(entity.getTokens());
        tokens.add(new TokenEntity(macaroon.identifier, entity, TokenEntity.TokenType.MACAROON));
        entity.setTokens(tokens);
        this.dao.updateOrganization(entity);

        // Return the base64 encoded Macaroon
        return new String(this.bakery.serializeMacaroon(macaroon, true), StandardCharsets.UTF_8);
    }

    @Override
    @GET
    @Path("/{organizationID}/token/verify")
    public Response verifyOrganizationToken(@PathParam("organizationID") UUID organizationID, @QueryParam("token") String token) {
        final boolean valid = validateMacaroon(organizationID, parseToken(token));
        if (valid) {
            return Response.ok().build();
        }

        return Response.status(Response.Status.UNAUTHORIZED).build();
    }

    private boolean validateMacaroon(UUID organizationID, Macaroon macaroon) {
        try {
            final String caveatString = String.format("organization_id = %s", organizationID.toString());
            this.bakery.verifyMacaroon(macaroon, caveatString);
        } catch (BakeryException e) {
            logger.error("Macaroon verification failed.", e);
            return false;
        }

        return true;
    }

    private Macaroon generateMacaroon(UUID organizationID) {
        // Create some caveats
        final List<MacaroonCaveat> caveats = List.of(
                new MacaroonCaveat("organization_id", MacaroonCaveat.Operator.EQ, organizationID.toString())
        );
        return this.bakery.createMacaroon(caveats);
    }

    private Macaroon parseToken(String token) {
        if (token == null || Objects.equals(token, "")) {
            throw new WebApplicationException("Cannot have empty token string", Response.Status.BAD_REQUEST);
        }
        return this.bakery.deserializeMacaroon(token);
    }

    private Macaroon parseTokenTag(String tokenTag) {
        final int idx = tokenTag.indexOf('|');
        if (idx <= 0) {
            throw new WebApplicationException("Malformed tokenTag", Response.Status.BAD_REQUEST);
        }

        return bakery.deserializeMacaroon(tokenTag.substring(idx + 1));
    }
}
