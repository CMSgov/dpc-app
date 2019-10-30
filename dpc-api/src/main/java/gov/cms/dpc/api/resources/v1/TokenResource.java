package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.github.nitram509.jmacaroons.Macaroon;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.entities.TokenEntity;
import gov.cms.dpc.api.jdbi.TokenDAO;
import gov.cms.dpc.api.models.TokenResponse;
import gov.cms.dpc.api.resources.AbstractTokenResource;
import gov.cms.dpc.macaroons.MacaroonBakery;
import gov.cms.dpc.macaroons.MacaroonCaveat;
import gov.cms.dpc.macaroons.MacaroonCondition;
import gov.cms.dpc.macaroons.config.TokenPolicy;
import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import io.dropwizard.jersey.jsr310.OffsetDateTimeParam;
import io.swagger.annotations.*;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.dstu3.model.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Api(tags = {"Auth", "Token"}, authorizations = @Authorization(value = "apiKey"))
public class TokenResource extends AbstractTokenResource {

    private static final Logger logger = LoggerFactory.getLogger(TokenResource.class);
    private static final String ORG_NOT_FOUND = "Cannot find Organization: %s";

    private final TokenDAO dao;
    private final MacaroonBakery bakery;
    private final TokenPolicy policy;
    private final IGenericClient client;

    @Inject
    TokenResource(TokenDAO dao, MacaroonBakery bakery, TokenPolicy policy, IGenericClient client) {
        this.dao = dao;
        this.bakery = bakery;
        this.policy = policy;
        this.client = client;
    }

    @Override
    @GET
    @UnitOfWork
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Fetch client tokens", notes = "Method to retrieve the client tokens associated to the given Organization.")
    @ApiResponses(value = @ApiResponse(code = 404, message = "Could not find Organization", response = OperationOutcome.class))
    public List<TokenEntity> getOrganizationTokens(
            @ApiParam(hidden = true) @Auth OrganizationPrincipal organizationPrincipal) {
        return this.dao.fetchTokens(organizationPrincipal.getID());
    }

    @GET
    @Path("/{tokenID}")
    @UnitOfWork
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Fetch client token", notes = "Method to retrieve metadata for a specific access token")
    @ApiResponses(value = @ApiResponse(code = 404, message = "Could not find Token", response = OperationOutcome.class))
    @Override
    public TokenEntity getOrganizationToken(@ApiParam(hidden = true) @Auth OrganizationPrincipal principal,
                                            @ApiParam(value = "Token ID", required = true) @NotNull @QueryParam("tokenID") UUID tokenID) {
        final List<TokenEntity> tokens = this.dao.findTokenByOrgAndID(principal.getID(), tokenID);
        if (tokens.isEmpty()) {
            throw new WebApplicationException("Cannot find token with matching ID", Response.Status.NOT_FOUND);
        }

        // Return the first token, since we know that IDs are unique
        return tokens.get(0);
    }

    @POST
    @Path("/{organizationID}")
    @UnitOfWork
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Create authentication token", notes = "Create a new authentication token for the given Organization (identified by Resource ID)." +
            "<p>" +
            "Token supports a custom human-readable label via the `label` query param.")
    @Override
    public TokenEntity createOrganizationToken(
            @ApiParam(hidden = true) @Auth OrganizationPrincipal organizationPrincipal,
            @ApiParam(value = "Organization resource ID", required = true)
            @NotNull @PathParam("organizationID") UUID organizationID,
            @ApiParam(value = "Optional label for token") @QueryParam("label") String tokenLabel, @QueryParam("expiration") Optional<OffsetDateTimeParam> expiration) {
        checkOrganizationMatches(organizationPrincipal, organizationID);

        // Verify that the organization actually exists
        try {
            this.client
                    .read()
                    .resource(Organization.class)
                    .withId(organizationID.toString())
                    .encodedJson()
                    .execute();
        } catch (ResourceNotFoundException e) {
            throw new WebApplicationException("Cannot find organization", Response.Status.NOT_FOUND);
        }

        final Macaroon macaroon = generateMacaroon(organizationID);

        // Ensure that each generated Macaroon has an associated Organization ID
        // This way we check to make sure we never generate a Golden Macaroon
        ensureOrganizationPresent(macaroon);

        final TokenEntity token = new TokenEntity(macaroon.identifier, organizationID, TokenEntity.TokenType.MACAROON);

        // Set the expiration time
        token.setExpiresAt(handleExpirationTime(expiration));

        // Set the label, if provided, otherwise, generate a default one
        token.setLabel(Optional.ofNullable(tokenLabel).orElse(String.format("Token for organization %s.", organizationID)));
        logger.info("Generating access token: {}", token);
        final TokenEntity persisted;
        try {
            persisted = this.dao.persistToken(token);
        } catch (NoResultException e) {
            throw new WebApplicationException(String.format(ORG_NOT_FOUND, organizationID), Response.Status.NOT_FOUND);
        }

        persisted.setToken(new String(this.bakery.serializeMacaroon(macaroon, true), StandardCharsets.UTF_8));
        return persisted;
    }

    @Override
    @DELETE
    @Path("/{tokenID}")
    @UnitOfWork
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Delete authentication token", notes = "Delete the specified authentication token for the given Organization (identified by Resource ID)")
    public Response deleteOrganizationToken(
            @ApiParam(hidden = true) @Auth OrganizationPrincipal organizationPrincipal,
            @ApiParam(value = "Token ID", required = true) @NotNull @PathParam("tokenID") UUID tokenID) {
        final List<TokenEntity> matchedToken = this.dao.findTokenByOrgAndID(organizationPrincipal.getID(), tokenID);
        assert matchedToken.size() == 1 : "Should only have a single matching token";

        this.dao.deleteToken(matchedToken.get(0));

        return Response.ok().build();
    }

    private Macaroon generateMacaroon(UUID organizationID) {
        // Create some caveats
        final List<MacaroonCaveat> caveats = List.of(
                new MacaroonCaveat("", new MacaroonCondition("organization_id", MacaroonCondition.Operator.EQ, organizationID.toString()))
        );
        return this.bakery.createMacaroon(caveats);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private OffsetDateTime handleExpirationTime(Optional<OffsetDateTimeParam> expiresParam) {
        // Compute default expiration
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        final OffsetDateTime defaultExpiration = now.plus(this.policy.getExpirationPolicy().getExpirationOffset(), this.policy.getExpirationPolicy().getExpirationUnit());

        // If a custom expiration is supplied use it, unless it violates our default policy
        if (expiresParam.isPresent()) {
            final OffsetDateTime customExpiration = expiresParam.get().get();

            // Verify custom expiration is not greater than policy
            if (customExpiration.isAfter(defaultExpiration)) {
                throw new WebApplicationException("Cannot set expiration after policy default", Response.Status.BAD_REQUEST);
            }

            // Verify is not already expired
            if (customExpiration.isBefore(now)) {
                throw new WebApplicationException("Cannot set expiration before current time", Response.Status.BAD_REQUEST);
            }
            return customExpiration;
        }
        return defaultExpiration;
    }

    private void ensureOrganizationPresent(Macaroon macaroon) {
        final boolean idMissing = this.bakery
                .getCaveats(macaroon)
                .stream()
                .map(MacaroonCaveat::getCondition)
                .noneMatch(cond -> cond.getKey().equals("organization_id"));

        if (idMissing) {
            logger.error("GOLDEN MACAROON WAS GENERATED IN TOKEN RESOURCE!");
            // TODO: Remove the Macaroon from the root key store (DPC-729)
            throw new IllegalStateException("Token generation failed");
        }
    }

    private static void checkOrganizationMatches(OrganizationPrincipal organizationPrincipal, UUID organizationID) {
        if (!organizationPrincipal.getID().equals(organizationID)) {
            throw new WebApplicationException("Not authorized", Response.Status.UNAUTHORIZED);
        }
    }
}
