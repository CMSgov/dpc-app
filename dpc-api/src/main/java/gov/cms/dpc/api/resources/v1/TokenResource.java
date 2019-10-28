package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.github.nitram509.jmacaroons.Macaroon;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.auth.annotations.Public;
import gov.cms.dpc.api.auth.jwt.JTICache;
import gov.cms.dpc.api.entities.TokenEntity;
import gov.cms.dpc.api.jdbi.TokenDAO;
import gov.cms.dpc.api.models.JWTAuthResponse;
import gov.cms.dpc.api.resources.AbstractTokenResource;
import gov.cms.dpc.common.annotations.APIV1;
import gov.cms.dpc.macaroons.MacaroonBakery;
import gov.cms.dpc.macaroons.MacaroonCaveat;
import gov.cms.dpc.macaroons.MacaroonCondition;
import gov.cms.dpc.macaroons.config.TokenPolicy;
import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import io.dropwizard.jersey.jsr310.OffsetDateTimeParam;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SecurityException;
import io.swagger.annotations.*;
import org.hibernate.validator.constraints.NotEmpty;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.dstu3.model.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static gov.cms.dpc.macaroons.caveats.ExpirationCaveatSupplier.EXPIRATION_KEY;

@Api(tags = {"Auth", "Token"}, authorizations = @Authorization(value = "apiKey"))
public class TokenResource extends AbstractTokenResource {

    private static final Logger logger = LoggerFactory.getLogger(TokenResource.class);
    private static final String ORG_NOT_FOUND = "Cannot find Organization: %s";
    public static final String CLIENT_ASSERTION_TYPE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";
    private static final String INVALID_JWT_MSG = "Invalid JWT";

    private final TokenDAO dao;
    private final MacaroonBakery bakery;
    private final TokenPolicy policy;
    private final IGenericClient client;
    private final SigningKeyResolverAdapter resolver;
    private final JTICache cache;
    private final String authURL;

    @Inject
    public TokenResource(TokenDAO dao,
                         MacaroonBakery bakery,
                         TokenPolicy policy,
                         IGenericClient client,
                         SigningKeyResolverAdapter resolver,
                         JTICache cache,
                         @APIV1 String publicURl) {
        this.dao = dao;
        this.bakery = bakery;
        this.policy = policy;
        this.client = client;
        this.resolver = resolver;
        this.cache = cache;
        this.authURL = String.format("%s/Token/auth", publicURl);
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
            @ApiParam(hidden = true) @Auth OrganizationPrincipal organizationPrincipal,
            @ApiParam(value = "Organization resource ID", required = true)
            @PathParam("organizationID") UUID organizationID) {
        checkOrganizationMatches(organizationPrincipal, organizationID);
        return this.dao.fetchTokens(organizationID);
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
            @ApiParam(hidden = true) @Auth OrganizationPrincipal organizationPrincipal,
            @ApiParam(value = "Organization resource ID", required = true) @NotNull @PathParam("organizationID") UUID organizationID,
            @ApiParam(value = "Token ID", required = true) @NotNull @PathParam("tokenID") UUID tokenID) {
        checkOrganizationMatches(organizationPrincipal, organizationID);
        final List<TokenEntity> matchedToken = this.dao.findTokenByOrgAndID(organizationID, tokenID);
        assert matchedToken.size() == 1 : "Should only have a single matching token";

        this.dao.deleteToken(matchedToken.get(0));

        return Response.ok().build();
    }

    @POST
    @Path("/auth")
    @UnitOfWork
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Delete authentication token", notes = "Delete the specified authentication token for the given Organization (identified by Resource ID)")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Public
    @Override
    public JWTAuthResponse authorizeJWT(@QueryParam(value = "scope") @NotEmpty(message = "Scope is required") String scope,
                                        @QueryParam(value = "grant_type") @NotEmpty(message = "Grant type is required") String grantType,
                                        @QueryParam(value = "client_assertion_type") @NotEmpty(message = "Assertion type is required") String clientAssertionType, @QueryParam(value = "client_assertion") String jwtBody) {
        validateJWTQueryParams(grantType, clientAssertionType);

        // Validate JWT signature
        try {
            return handleJWT(jwtBody);
        } catch (SecurityException e) {
            logger.error("JWT has invalid signature", e);
            throw new WebApplicationException(INVALID_JWT_MSG, Response.Status.UNAUTHORIZED);
        } catch (JwtException e) {
            logger.error("Malformed JWT", e);
            throw new WebApplicationException(INVALID_JWT_MSG, Response.Status.UNAUTHORIZED);
        }
    }

    private void validateJWTQueryParams(String grantType, String clientAssertionType) {
        if (!grantType.equals("client_credentials")) {
            throw new WebApplicationException("Grant Type must be 'client_credentials'", Response.Status.BAD_REQUEST);
        }

        if (!clientAssertionType.equals(CLIENT_ASSERTION_TYPE)) {
            throw new WebApplicationException(String.format("Client Assertion Type must be '%s'", CLIENT_ASSERTION_TYPE), Response.Status.BAD_REQUEST);
        }
    }

    private JWTAuthResponse handleJWT(String jwtBody) {
        final Jws<Claims> claims = Jwts.parser()
                .setSigningKeyResolver(this.resolver)
                .requireAudience(this.authURL)
                .parseClaimsJws(jwtBody);

        // Determine if claims are present and valid
        // Required claims are specified here: http://hl7.org/fhir/us/bulkdata/2019May/authorization/index.html#protocol-details
        // TODO: wire in the real Organization ID, for auditing purposes
        handleJWTClaims(UUID.randomUUID(), claims);

        // Extract the Client Macaroon from the subject field (which is the same as the issuer)
        final String clientMacaroon = claims.getBody().getSubject();
        final List<Macaroon> macaroons = this.bakery.deserializeMacaroon(clientMacaroon);

        // Add the additional claims that we need
        // Currently, we need to set an expiration time, a set of scopes,
        final Duration tokenLifetime = Duration.of(5, ChronoUnit.MINUTES);
        final OffsetDateTime expiryTime = OffsetDateTime.now(ZoneOffset.UTC)
                .plus(tokenLifetime);

        // Add an additional restriction to the root Macaroons
        final Macaroon restrictedMacaroon = this.bakery.addCaveats(macaroons.get(0), new MacaroonCaveat(new MacaroonCondition(EXPIRATION_KEY, MacaroonCondition.Operator.EQ, expiryTime.toString())));

        final List<Macaroon> discharged = this.bakery.dischargeAll(Collections.singletonList(restrictedMacaroon), this.bakery::discharge);
        final JWTAuthResponse response = new JWTAuthResponse();
        response.setExpiresIn(tokenLifetime);
        response.setDischargedMacaroons(new String(this.bakery.serializeMacaroon(discharged, true), StandardCharsets.UTF_8));

        return response;
    }

    private Macaroon generateMacaroon(UUID organizationID) {
        // Create some caveats
        final List<MacaroonCaveat> caveats = List.of(
                new MacaroonCaveat("", new MacaroonCondition("organization_id", MacaroonCondition.Operator.EQ, organizationID.toString())),
                new MacaroonCaveat("local", new MacaroonCondition("organization_id", MacaroonCondition.Operator.EQ, organizationID.toString()))
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

    private void handleJWTClaims(UUID organizationID, Jws<Claims> claims) {
        // Issuer and Sub must be present and identical
        final String issuer = getClaimIfPresent("issuer", claims.getBody().getIssuer());
        final String subject = getClaimIfPresent("subject", claims.getBody().getSubject());
        if (!issuer.equals(subject)) {
            throw new WebApplicationException("Issuer and Subject must be identical", Response.Status.BAD_REQUEST);
        }

        // JTI must be present and have not been used in the past 5 minutes.
        if (!this.cache.isJTIOk(getClaimIfPresent("id", claims.getBody().getId()))) {
            logger.warn("JWT being replayed for organization {}", organizationID);
            throw new WebApplicationException(INVALID_JWT_MSG, Response.Status.UNAUTHORIZED);
        }

        // Ensure the expiration time for the token is not more than 5 minutes in the future
        final Date expiration = getClaimIfPresent("expiration", claims.getBody().getExpiration());
        if (OffsetDateTime.now(ZoneOffset.UTC).plus(5, ChronoUnit.MINUTES).isBefore(expiration.toInstant().atOffset(ZoneOffset.UTC))) {
            throw new WebApplicationException("Not authorized", Response.Status.UNAUTHORIZED);
        }
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

    private static <T> T getClaimIfPresent(String claimName, @Nullable T claim) {
        if (claim == null) {
            throw new WebApplicationException(String.format("Claim %s must be present", claimName), Response.Status.BAD_REQUEST);
        }
        return claim;
    }
}
