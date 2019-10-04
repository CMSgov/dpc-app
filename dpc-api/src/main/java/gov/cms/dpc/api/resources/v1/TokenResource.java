package gov.cms.dpc.api.resources.v1;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.github.nitram509.jmacaroons.Macaroon;
import gov.cms.dpc.api.DPCAPIConfiguration;
import gov.cms.dpc.macaroons.config.TokenPolicy;
import gov.cms.dpc.api.jdbi.TokenDAO;
import gov.cms.dpc.api.resources.AbstractTokenResource;
import gov.cms.dpc.common.entities.OrganizationEntity;
import gov.cms.dpc.common.entities.TokenEntity;
import gov.cms.dpc.macaroons.MacaroonBakery;
import gov.cms.dpc.macaroons.MacaroonCaveat;
import gov.cms.dpc.macaroons.MacaroonCondition;
import gov.cms.dpc.macaroons.exceptions.BakeryException;
import io.dropwizard.hibernate.UnitOfWork;
import io.dropwizard.jersey.jsr310.OffsetDateTimeParam;
import io.swagger.annotations.*;
import org.eclipse.jetty.http.HttpStatus;
import org.hibernate.validator.constraints.NotEmpty;
import org.hl7.fhir.dstu3.model.OperationOutcome;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Api(value = "Token")
public class TokenResource extends AbstractTokenResource {

    private static final Logger logger = LoggerFactory.getLogger(TokenResource.class);
    private static final String ORG_NOT_FOUND = "Cannot find Organization: %s";

    private final TokenDAO dao;
    private final MacaroonBakery bakery;
    private final TokenPolicy policy;

    @Inject
    TokenResource(TokenDAO dao, MacaroonBakery bakery, DPCAPIConfiguration config) {
        this.dao = dao;
        this.bakery = bakery;
        this.policy = config.getTokenPolicy();
    }

    @Override
    @GET
    @Path("/{organizationID}")
    @UnitOfWork
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Fetch organization tokens", notes = "Method to retrieve the authentication tokens associated to the given Organization. This searches by resource ID")
    @ApiResponses(value = @ApiResponse(code = 404, message = "Could not find Organization", response = OperationOutcome.class))
    public List<TokenEntity> getOrganizationTokens(
            @ApiParam(value = "Organization resource ID", required = true)
            @PathParam("organizationID") UUID organizationID) {
        return this.dao.fetchTokens(organizationID);
    }

    @Override
    @GET
    @Timed
    @ExceptionMetered
    @Path("/{organizationID}/verify")
    @ApiOperation(value = "Verify authentication token", notes = "Verify an authentication token with a given Organization. " +
            "This allows for checking if a given token is correctly to the organization if the token is valid.")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Token cannot be empty"),
            @ApiResponse(code = 401, message = "Token is not valid for the given Organization"),
            @ApiResponse(code = 422, message = "Token is malformed")
    })
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

    @POST
    @Path("/{organizationID}")
    @UnitOfWork
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Create authentication token", notes = "Create a new authentication token for the given Organization (identified by Resource ID)." +
            "<p>" +
            "Token supports a custom human-readable label via the `label` query param.")
    @Override
    public String createOrganizationToken(
            @ApiParam(value = "Organization resource ID", required = true)
            @NotNull @PathParam("organizationID") UUID organizationID,
            @ApiParam(value = "Optional label for token") @QueryParam("label") String tokenLabel, @QueryParam("expiration") Optional<OffsetDateTimeParam> expiration) {

        final Macaroon macaroon = generateMacaroon(organizationID);

        final OrganizationEntity organization = new OrganizationEntity();
        organization.setId(organizationID);

        final TokenEntity token = new TokenEntity(macaroon.identifier, organization, TokenEntity.TokenType.MACAROON);

        // Set the expiration time
        token.setExpiresAt(handleExpirationTime(expiration));

        // Set the label, if provided, otherwise, generate a default one
        token.setLabel(Optional.ofNullable(tokenLabel).orElse(String.format("Token for organization %s.", organizationID)));
        logger.info("Generating access token: {}", token);
        try {
            this.dao.persistToken(token);
        } catch (NoResultException e) {
            throw new WebApplicationException(String.format(ORG_NOT_FOUND, organizationID), Response.Status.NOT_FOUND);
        }

        return new String(this.bakery.serializeMacaroon(macaroon, true), StandardCharsets.UTF_8);
    }

    @Override
    @DELETE
    @Path("/{organizationID}/{tokenID}")
    @UnitOfWork
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Delete authentication token", notes = "Delete the specified authentication token for the given Organization (identified by Resource ID)")
    public Response deleteOrganizationToken(
            @ApiParam(value = "Organization resource ID", required = true) @NotNull @PathParam("organizationID") UUID organizationID,
            @ApiParam(value = "Token ID", required = true) @NotNull @PathParam("tokenID") UUID tokenID) {

        final List<TokenEntity> matchedToken = this.dao.findTokenByOrgAndID(organizationID, tokenID);
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

    private Macaroon parseMacaroonToken(String token) {
        try {
            return this.bakery.deserializeMacaroon(token);
        } catch (BakeryException e) {
            logger.error("Cannot deserialize Macaroon", e);
            throw new WebApplicationException("Cannot deserialize Macaroon", HttpStatus.UNPROCESSABLE_ENTITY_422);
        }
    }

    private boolean validateMacaroon(UUID organizationID, Macaroon macaroon) {
        try {
            final String caveatString = String.format("organization_id = %s", organizationID.toString());
            this.bakery.verifyMacaroon(Collections.singletonList(macaroon), caveatString);
        } catch (BakeryException e) {
            logger.error("Macaroon verification failed.", e);
            return false;
        }
        return true;
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
}
