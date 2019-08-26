package gov.cms.dpc.attribution.resources.v1;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.github.nitram509.jmacaroons.Macaroon;
import gov.cms.dpc.attribution.jdbi.OrganizationDAO;
import gov.cms.dpc.common.models.TokenResponse;
import gov.cms.dpc.attribution.resources.AbstractOrganizationResource;
import gov.cms.dpc.common.entities.EndpointEntity;
import gov.cms.dpc.common.entities.OrganizationEntity;
import gov.cms.dpc.common.entities.TokenEntity;
import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.converters.EndpointConverter;
import gov.cms.dpc.macaroons.MacaroonBakery;
import gov.cms.dpc.macaroons.MacaroonCaveat;
import gov.cms.dpc.macaroons.exceptions.BakeryException;
import io.dropwizard.hibernate.UnitOfWork;
import io.swagger.annotations.*;
import org.eclipse.jetty.http.HttpStatus;
import org.hibernate.validator.constraints.NotEmpty;
import org.hl7.fhir.dstu3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static gov.cms.dpc.attribution.utils.RESTUtils.parseTokenTag;

@Api(value = "Organization")
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
    @FHIR
    @UnitOfWork
    @ApiOperation(value = "Search and Validate Token",
            notes = "FHIR Endpoint to find an Organization resource associated to the given authentication token." +
                    "<p>This also validates that the token is valid." +
                    "<p>The *_tag* parameter is used to convey the token, which is half-way between FHIR and REST.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Organization matching (valid) token was found."),
            @ApiResponse(code = 404, message = "Organization was not found matching token", response = OperationOutcome.class),
            @ApiResponse(code = 401, message = "Organization was found, but token was invalid", response = OperationOutcome.class)
    })
    public Bundle searchOrganizations(
            @ApiParam(value = "NPI of Organization")
            @QueryParam("identifier") String identifier,
            @ApiParam(value = "Authorization token to validate")
            @QueryParam("_tag") String tokenTag) {
        if (tokenTag != null) {
            return searchAndValidationByToken(tokenTag);
        }

        final Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);

        if (identifier == null) {
            final List<OrganizationEntity> organizationEntityList = this.dao.listOrganizations();
            bundle.setTotal(organizationEntityList.size());

            organizationEntityList.forEach(entity -> bundle.addEntry().setResource(entity.toFHIR()));
            return bundle;
        }

        final List<OrganizationEntity> queryList = this.dao.searchByIdentifier(identifier);

        if (!queryList.isEmpty()) {
            bundle.setTotal(queryList.size());
            queryList.forEach(org -> bundle.addEntry().setResource(org.toFHIR()));
        }

        return bundle;
    }

    @Override
    @FHIR
    @UnitOfWork
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Create Organization", notes = "FHIR endpoint that accepts a Bundle resource containing an Organization and a list of Endpoint resources to register with the application")
    @ApiResponses(value = {
            @ApiResponse(code = 422, message = "Must provide a single Organization resource to register", response = OperationOutcome.class),
            @ApiResponse(code = 201, message = "Organization was successfully registered")
    })
    public Response submitOrganization(Parameters parameters) {

        final Parameters.ParametersParameterComponent firstRep = parameters.getParameterFirstRep();

        if (!firstRep.hasResource()) {
            throw new WebApplicationException("Must submit bundle", HttpStatus.UNPROCESSABLE_ENTITY_422);
        }
        final Bundle transactionBundle = (Bundle) firstRep.getResource();

        final Optional<Organization> organization = transactionBundle
                .getEntry()
                .stream()
                .filter(entry -> entry.hasResource() && entry.getResource().getResourceType() == ResourceType.Organization)
                .map(entry -> (Organization) entry.getResource())
                .findFirst();

        if (organization.isEmpty()) {
            return Response.status(HttpStatus.UNPROCESSABLE_ENTITY_422).entity("Must provide organization to register").build();
        }

        try {
            final Organization persistedOrg = this.dao.registerOrganization(organization.get(), extractEndpoints(transactionBundle));
            return Response.status(Response.Status.CREATED).entity(persistedOrg).build();
        } catch (Exception e) {
            logger.error("Error: ", e);
            throw e;
        }
    }

    @GET
    @Path("/{organizationID}")
    @FHIR
    @UnitOfWork
    @Override
    @ApiOperation(value = "Fetch organization", notes = "FHIR endpoint to fetch an Organization with the given Resource ID")
    @ApiResponses(value = @ApiResponse(code = 404, message = "Could not find Organization", response = OperationOutcome.class))
    public Organization getOrganization(
            @ApiParam(value = "Organization resource ID", required = true)
            @PathParam("organizationID") UUID organizationID) {
        final Optional<OrganizationEntity> orgOptional = this.dao.fetchOrganization(organizationID);
        final OrganizationEntity organizationEntity = orgOptional.orElseThrow(() -> new WebApplicationException(String.format("Cannot find organization '%s'", organizationID), Response.Status.NOT_FOUND));
        return organizationEntity.toFHIR();
    }

    @DELETE
    @Path("/{organizationID}")
    @FHIR
    @UnitOfWork
    @ApiOperation(value = "Delete organization", notes = "FHIR endpoint to deleta an Organization with the given Resource ID." +
            "<p>This also drops ALL resources associated to the given entity.")
    @ApiResponses(value = @ApiResponse(code = 404, message = "Could not find Organization", response = OperationOutcome.class))
    @Override
    public Response deleteOrganization(@ApiParam(value = "Organization resource ID", required = true) @PathParam("organizationID") UUID organizationID) {
        final OrganizationEntity organizationEntity = this.dao.fetchOrganization(organizationID)
                .orElseThrow(() -> new WebApplicationException("Cannot find organization.", Response.Status.NOT_FOUND));

        this.dao.deleteOrganization(organizationEntity);
        return Response.ok().build();
    }

    @GET
    @Path("/{organizationID}/token")
    @UnitOfWork
    @Override
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Fetch organization tokens", notes = "Method to retrieve the authentication tokens associated to the given Organization. This searches by resource ID")
    @ApiResponses(value = @ApiResponse(code = 404, message = "Could not find Organization", response = OperationOutcome.class))
    public List<TokenResponse> getOrganizationTokens(
            @ApiParam(value = "Organization resource ID", required = true)
            @PathParam("organizationID") UUID organizationID) {
        final Optional<OrganizationEntity> entityOptional = this.dao.fetchOrganization(organizationID);

        final OrganizationEntity entity = entityOptional.orElseThrow(() -> new WebApplicationException(String.format("Cannot find Organization: %s", organizationID), Response.Status.NOT_FOUND));

        return entity
                .getTokens()
                .stream()
                .map(te -> new TokenResponse(te.getId(), te.getTokenType(), "never"))
                .collect(Collectors.toList());
    }

    @POST
    @Path("/{organizationID}/token")
    @UnitOfWork
    @Override
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Create authentication token", notes = "Create a new authentication token for the given Organization (identified by Resource ID)")
    public String createOrganizationToken(
            @ApiParam(value = "Organization resource ID", required = true)
            @PathParam("organizationID") UUID organizationID) {
        final Optional<OrganizationEntity> entityOptional = this.dao.fetchOrganization(organizationID);

        final OrganizationEntity entity = entityOptional.orElseThrow(() -> new WebApplicationException(String.format("Cannot find Organization: %s", organizationID), Response.Status.NOT_FOUND));

        final Macaroon macaroon = generateMacaroon(organizationID);

        // Add the macaroon ID to the organization and update it
        entity.addToken(new TokenEntity(macaroon.identifier, entity, TokenEntity.TokenType.MACAROON));
        this.dao.updateOrganization(entity);

        // Return the base64 encoded Macaroon
        return new String(this.bakery.serializeMacaroon(macaroon, true), StandardCharsets.UTF_8);
    }

    @DELETE
    @Path("/{organizationID}/token/{tokenID}")
    @UnitOfWork
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Create authentication token", notes = "Create a new authentication token for the given Organization (identified by Resource ID)")
    @Override
    public Response deleteOrganizationToken(@NotNull @PathParam("organizationID") UUID organizationID, @NotNull @PathParam("tokenID") UUID tokenID) {
        final OrganizationEntity organizationEntity = this.dao.fetchOrganization(organizationID)
                .orElseThrow(() -> new WebApplicationException(String.format("Cannot find Organization: %s", organizationID), Response.Status.NOT_FOUND));

        final TokenEntity foundToken = organizationEntity
                .getTokens()
                .stream()
                .filter(token -> token.getId().equals(tokenID.toString()))
                .findAny()
                .orElseThrow(() -> new WebApplicationException("Cannot find token by ID", Response.Status.NOT_FOUND));

        organizationEntity.removeToken(foundToken);
        this.dao.updateOrganization(organizationEntity);

        return Response.ok().build();
    }

    @Override
    @GET
    @Timed
    @ExceptionMetered
    @Path("/{organizationID}/token/verify")
    @ApiOperation(value = "Verify authentication token", notes = "Verify an authentication token with a given Organization. " +
            "This allows for checking if a given token is correctly to the organization if the token is valid.")
    @ApiResponses(value = @ApiResponse(code = 401, message = "Token is not valid for the given Organization"))
    public Response verifyOrganizationToken(
            @ApiParam(value = "Organization resource ID", required = true)
            @PathParam("organizationID") UUID organizationID,
            @ApiParam(value = "Authentication token to verify", required = true)
            @NotEmpty @QueryParam("token") String token) {
        final boolean valid = validateMacaroon(organizationID, parseMacaroonToken(token));
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

    private Macaroon parseMacaroonToken(String token) {
        if (token == null || Objects.equals(token, "")) {
            throw new WebApplicationException("Cannot have empty token string", Response.Status.BAD_REQUEST);
        }
        return this.bakery.deserializeMacaroon(token);
    }

    private Bundle searchAndValidationByToken(String token) {
        final Macaroon macaroon = parseTokenTag(this.bakery::deserializeMacaroon, token);

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
        bundle.setTotal(1);

        return bundle;
    }

    private List<EndpointEntity> extractEndpoints(Bundle transactionBundle) {
        return transactionBundle
                .getEntry()
                .stream()
                .filter(entry -> entry.hasResource() && entry.getResource().getResourceType() == ResourceType.Endpoint)
                .map(entry -> (Endpoint) entry.getResource())
                .map(EndpointConverter::convert)
                .collect(Collectors.toList());
    }
}
