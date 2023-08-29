package gov.cms.dpc.api.resources.v1;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.github.nitram509.jmacaroons.Macaroon;
import gov.cms.dpc.api.auth.MacaroonHelpers;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.auth.annotations.Authorizer;
import gov.cms.dpc.api.auth.annotations.Public;
import gov.cms.dpc.api.auth.jwt.IJTICache;
import gov.cms.dpc.api.auth.jwt.ValidatingKeyResolver;
import gov.cms.dpc.api.entities.TokenEntity;
import gov.cms.dpc.api.jdbi.TokenDAO;
import gov.cms.dpc.api.models.CollectionResponse;
import gov.cms.dpc.api.models.JWTAuthResponse;
import gov.cms.dpc.api.models.CreateTokenRequest;
import gov.cms.dpc.api.resources.AbstractTokenResource;
import gov.cms.dpc.common.annotations.APIV1;
import gov.cms.dpc.common.annotations.NoHtml;
import gov.cms.dpc.macaroons.CaveatSupplier;
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
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.validation.Valid;
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
import java.util.stream.Collectors;

import static gov.cms.dpc.api.auth.MacaroonHelpers.ORGANIZATION_CAVEAT_KEY;
import static gov.cms.dpc.api.auth.MacaroonHelpers.generateCaveatsForToken;
import static gov.cms.dpc.macaroons.caveats.ExpirationCaveatSupplier.EXPIRATION_KEY;

@Api(tags = {"Auth", "Token"}, authorizations = @Authorization(value = "access_token"))
@Path("/v1/Token")
public class TokenResource extends AbstractTokenResource {

    public static final String CLIENT_ASSERTION_TYPE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";
    // This will be removed as part of DPC-747
    private static final String DEFAULT_ACCESS_SCOPE = "system/*.*";
    private static final Logger logger = LoggerFactory.getLogger(TokenResource.class);
    private static final String INVALID_JWT_MSG = "Invalid JWT";

    private final TokenDAO dao;
    private final MacaroonBakery bakery;
    private final TokenPolicy policy;
    private final SigningKeyResolverAdapter resolver;
    private final IJTICache cache;
    private final String authURL;

    @Inject
    public TokenResource(TokenDAO dao,
                         MacaroonBakery bakery,
                         TokenPolicy policy,
                         SigningKeyResolverAdapter resolver,
                         IJTICache cache,
                         @APIV1 String publicURL) {
        this.dao = dao;
        this.bakery = bakery;
        this.policy = policy;
        this.resolver = resolver;
        this.cache = cache;
        this.authURL = String.format("%s/Token/auth", publicURL);
    }

    @Override
    @GET
    @UnitOfWork
    @Timed
    @ExceptionMetered
    @Authorizer
    @ApiOperation(value = "Fetch client tokens", notes = "Method to retrieve the client tokens associated to the given Organization.")
    @ApiResponses(value = @ApiResponse(code = 404, message = "Could not find Organization"))
    public CollectionResponse<TokenEntity> getOrganizationTokens(
            @ApiParam(hidden = true) @Auth OrganizationPrincipal organizationPrincipal) {
        return new CollectionResponse<>(this.dao.fetchTokens(organizationPrincipal.getID()));
    }

    @GET
    @Path("/{tokenID}")
    @UnitOfWork
    @Timed
    @ExceptionMetered
    @Authorizer
    @ApiOperation(value = "Fetch client token", notes = "Method to retrieve metadata for a specific access token")
    @ApiResponses(value = @ApiResponse(code = 404, message = "Could not find Token", response = OperationOutcome.class))
    @Override
    public TokenEntity getOrganizationToken(@ApiParam(hidden = true) @Auth OrganizationPrincipal principal,
                                            @ApiParam(value = "Token ID", required = true) @NotNull @PathParam("tokenID") UUID tokenID) {
        final List<TokenEntity> tokens = this.dao.findTokenByOrgAndID(principal.getID(), tokenID);
        if (tokens.isEmpty()) {
            throw new WebApplicationException("Cannot find token with matching ID", Response.Status.NOT_FOUND);
        }

        // Return the first token, since we know that IDs are unique
        return tokens.get(0);
    }

    @POST
    @UnitOfWork
    @Timed
    @ExceptionMetered
    @Authorizer
    @ApiOperation(value = "Create authentication token", notes = "Create a new authentication token for the given Organization (identified by Resource ID)." +
            "<p>" +
            "Token supports a custom human-readable label via the `label` query param as well as a custom expiration period via the `expiration` param." +
            "<p>" +
            "Note: The expiration time cannot exceed the maximum lifetime specified by the system (current 1 year)")
    @ApiResponses(value = @ApiResponse(code = 400, message = "Cannot create token with specified parameters"))
    @Override
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public TokenEntity createOrganizationToken(
            @ApiParam(hidden = true) @Auth OrganizationPrincipal organizationPrincipal, @Valid CreateTokenRequest body,
            @ApiParam(value = "Optional label for token") @QueryParam("label") @NoHtml String tokenLabel, @QueryParam("expiration") Optional<OffsetDateTimeParam> expiration) {

        final UUID organizationID = organizationPrincipal.getID();

        final Macaroon macaroon = generateMacaroon(this.policy, organizationID);
        final Optional<CreateTokenRequest> optionalBody = Optional.ofNullable(body);

        // Ensure that each generated Macaroon has an associated Organization ID
        // This way we check to make sure we never generate a Golden Macaroon
        ensureOrganizationPresent(macaroon);

        final TokenEntity tokenEntity = new TokenEntity(macaroon.identifier, organizationID, TokenEntity.TokenType.MACAROON);

        // Set the expiration time
        tokenEntity.setExpiresAt(handleExpirationTime(expiration, optionalBody));

        //Set the label, if provided, otherwise, generate a default one
        if(optionalBody.isPresent() && StringUtils.isNotEmpty(optionalBody.get().getLabel())){
            tokenEntity.setLabel(optionalBody.get().getLabel());
        }else{
            tokenEntity.setLabel(Optional.ofNullable(tokenLabel).orElse(String.format("Token for organization %s.", organizationID)));
        }
        logger.info("Generating access token: {}", tokenEntity);
        final TokenEntity persisted;
        try {
            persisted = this.dao.persistToken(tokenEntity);
        } catch (NoResultException e) {
            throw new WebApplicationException(String.format("Cannot find Organization: %s", organizationID), Response.Status.NOT_FOUND);
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
    @Authorizer
    @ApiOperation(value = "Delete authentication token", notes = "Delete the specified authentication token for the given Organization (identified by Resource ID)")
    @ApiResponses({@ApiResponse(code = 204, message = "Successfully deleted token"), @ApiResponse(code = 404, message = "Unable to find token with given id")})
    public Response deleteOrganizationToken(
            @ApiParam(hidden = true) @Auth OrganizationPrincipal organizationPrincipal,
            @ApiParam(value = "Token ID", required = true) @NotNull @PathParam("tokenID") UUID tokenID) {
        final List<TokenEntity> matchedToken = this.dao.findTokenByOrgAndID(organizationPrincipal.getID(), tokenID);
        assert matchedToken.size() == 1 : "Should only have a single matching token";

        this.dao.deleteToken(matchedToken.get(0));

        return Response.noContent().build();
    }

    @POST
    @Path("/auth")
    @UnitOfWork
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Request API access token", notes = "Request access token for API access")
    @ApiResponses(
            value = {@ApiResponse(code = 400, message = "Token request is invalid"),
                    @ApiResponse(code = 401, message = "Client is not authorized to request access token")})
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    @Public
    @Override
    public JWTAuthResponse authorizeJWT(
            @ApiParam(name = "scope", allowableValues = "system/*.*", value = "Requested access scope", required = true)
            @FormParam(value = "scope") @NoHtml @NotEmpty(message = "Scope is required") String scope,
            @ApiParam(name = "grant_type", value = "Authorization grant type", required = true, allowableValues = "client_credentials")
            @FormParam(value = "grant_type") @NoHtml @NotEmpty(message = "Grant type is required") String grantType,
            @ApiParam(name = "client_assertion_type", value = "Client Assertion Type", required = true, allowableValues = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer")
            @FormParam(value = "client_assertion_type") @NoHtml @NotEmpty(message = "Assertion type is required") String clientAssertionType,
            @ApiParam(name = "client_assertion", value = "Signed JWT", required = true)
            @FormParam(value = "client_assertion") @NoHtml @NotEmpty(message = "Assertion is required") String jwtBody) {
        // Actual scope implementation will come as part of DPC-747
        validateJWTQueryParams(grantType, clientAssertionType, scope, jwtBody);

        // Validate JWT signature
        try {
            return handleJWT(jwtBody, scope);
        } catch (SecurityException e) {
            logger.error("JWT has invalid signature", e);
            throw new WebApplicationException(INVALID_JWT_MSG, Response.Status.UNAUTHORIZED);
        } catch (JwtException e) {
            logger.error("Malformed JWT", e);
            throw new WebApplicationException(INVALID_JWT_MSG, Response.Status.UNAUTHORIZED);
        }
    }

    @POST
    @Path("/validate")
    @UnitOfWork
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Validate API token request", notes = "Validates a given JWT to ensure the required claims and values are set correctly.", authorizations = @Authorization(value = ""))
    @ApiResponses(
            value = {@ApiResponse(code = 200, message = "Token request is valid"),
                    @ApiResponse(code = 400, message = "Token request is invalid")})
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @Public
    @Override
    public Response validateJWT(@NoHtml @NotEmpty(message = "Must submit JWT") String jwt) {

        try {
            Jwts.parserBuilder()
                    .requireAudience(this.authURL)
                    .setSigningKeyResolver(new ValidatingKeyResolver(this.cache, this.authURL))
                    .build()
                    .parseClaimsJws(jwt);
        } catch (IllegalArgumentException e) {
            // This is fine, we just want the body
        } catch (MalformedJwtException e) {
            throw new WebApplicationException("JWT is not formatted correctly", Response.Status.BAD_REQUEST);
        }

        return Response.ok().build();
    }

    private void validateJWTQueryParams(String grantType, String clientAssertionType, String scope, String jwtBody) {
        if (!grantType.equals("client_credentials")) {
            throw new WebApplicationException("Grant Type must be 'client_credentials'", Response.Status.BAD_REQUEST);
        }

        if (!clientAssertionType.equals(CLIENT_ASSERTION_TYPE)) {
            throw new WebApplicationException(String.format("Client Assertion Type must be '%s'", CLIENT_ASSERTION_TYPE), Response.Status.BAD_REQUEST);
        }

        if (!scope.equals(DEFAULT_ACCESS_SCOPE)) {
            throw new WebApplicationException(String.format("Access Scope must be '%s'", DEFAULT_ACCESS_SCOPE), Response.Status.BAD_REQUEST);
        }

        if (jwtBody == null || jwtBody.isEmpty()) {
            throw new WebApplicationException("Client Assertion must be present", Response.Status.BAD_REQUEST);
        }
    }

    private JWTAuthResponse handleJWT(String jwtBody, String requestedScope) {
        final Jws<Claims> claims = Jwts.parserBuilder()
                .setSigningKeyResolver(this.resolver)
                .requireAudience(this.authURL)
                .build()
                .parseClaimsJws(jwtBody);

        // Extract the Client Macaroon from the subject field (which is the same as the issuer)
        final String clientMacaroon = claims.getBody().getSubject();
        final List<Macaroon> macaroons = MacaroonBakery.deserializeMacaroon(clientMacaroon);

        // Get org id from macaroon caveats
        UUID orgId = MacaroonHelpers.extractOrgIDFromCaveats(macaroons).orElseThrow(() -> {
            logger.error("No organization found on macaroon");
            throw new WebApplicationException(INVALID_JWT_MSG, Response.Status.UNAUTHORIZED);
        });

        // Determine if claims are present and valid
        // Required claims are specified here: http://hl7.org/fhir/us/bulkdata/2019May/authorization/index.html#protocol-details
        handleJWTClaims(orgId, claims);

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
        response.setScope(requestedScope);
        response.setDischargedMacaroons(new String(this.bakery.serializeMacaroon(discharged, true), StandardCharsets.UTF_8));

        return response;
    }

    private Macaroon generateMacaroon(TokenPolicy policy, UUID organizationID) {
        // Create some caveats
        final Duration tokenLifetime = policy
                .getExpirationPolicy().getExpirationUnit()
                .getDuration().multipliedBy(policy.getExpirationPolicy().getExpirationOffset());
        final List<MacaroonCaveat> caveats = generateCaveatsForToken(policy.getVersionPolicy().getCurrentVersion(), organizationID, tokenLifetime)
                .stream()
                .map(CaveatSupplier::get)
                .collect(Collectors.toList());
        return this.bakery.createMacaroon(caveats);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private OffsetDateTime handleExpirationTime(Optional<OffsetDateTimeParam> expiresQueryParam, Optional<CreateTokenRequest> token) {
        // Compute default expiration
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        final OffsetDateTime defaultExpiration = now.plus(this.policy.getExpirationPolicy().getExpirationOffset(), this.policy.getExpirationPolicy().getExpirationUnit());
        final OffsetDateTime expiresBodyParam = token.map(CreateTokenRequest::getExpiresAt).orElse(null);

        // If a custom expiration is supplied use it, unless it violates our default policy
        if (expiresQueryParam.isPresent() || expiresBodyParam!=null) {
            final OffsetDateTime customExpiration = expiresBodyParam!=null ? expiresBodyParam:expiresQueryParam.get().get();

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

    @SuppressWarnings("JdkObsolete") // Date class is used by Jwt
    private void handleJWTClaims(UUID organizationID, Jws<Claims> claims) {
        // Issuer and Sub must be present and identical
        final String issuer = getClaimIfPresent("issuer", claims.getBody().getIssuer());
        final String subject = getClaimIfPresent("subject", claims.getBody().getSubject());
        if (!issuer.equals(subject)) {
            throw new WebApplicationException("Issuer and Subject must be identical", Response.Status.BAD_REQUEST);
        }

        // JTI must be present and have not been used in the past 5 minutes.
        if (!this.cache.isJTIOk(getClaimIfPresent("id", claims.getBody().getId()), true)) {
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
        final boolean idMissing = MacaroonBakery
                .getCaveats(macaroon)
                .stream()
                .map(MacaroonCaveat::getCondition)
                .noneMatch(cond -> cond.getKey().equals(ORGANIZATION_CAVEAT_KEY));

        if (idMissing) {
            logger.error("GOLDEN MACAROON WAS GENERATED IN TOKEN RESOURCE!");
            // TODO: Remove the Macaroon from the root key store (DPC-729)
            throw new IllegalStateException("Token generation failed");
        }
    }

    private static <T> T getClaimIfPresent(String claimName, @Nullable T claim) {
        if (claim == null) {
            throw new WebApplicationException(String.format("Claim %s must be present", claimName), Response.Status.BAD_REQUEST);
        }
        return claim;
    }
}
