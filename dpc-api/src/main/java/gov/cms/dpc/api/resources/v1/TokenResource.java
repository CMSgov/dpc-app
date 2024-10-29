package gov.cms.dpc.api.resources.v1;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.github.nitram509.jmacaroons.Macaroon;
import com.github.nitram509.jmacaroons.NotDeSerializableException;
import gov.cms.dpc.api.auth.MacaroonHelpers;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.auth.annotations.Authorizer;
import gov.cms.dpc.common.annotations.Public;
import gov.cms.dpc.api.auth.jwt.IJTICache;
import gov.cms.dpc.api.auth.jwt.ValidatingKeyResolver;
import gov.cms.dpc.api.entities.TokenEntity;
import gov.cms.dpc.api.jdbi.TokenDAO;
import gov.cms.dpc.api.jdbi.PublicKeyDAO;
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
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.persistence.NoResultException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

import static gov.cms.dpc.api.auth.MacaroonHelpers.ORGANIZATION_CAVEAT_KEY;
import static gov.cms.dpc.api.auth.MacaroonHelpers.generateCaveatsForToken;
import static gov.cms.dpc.macaroons.caveats.ExpirationCaveatSupplier.EXPIRATION_KEY;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Api(tags = {"Auth", "Token"}, authorizations = @Authorization(value = "access_token"))
@Path("/v1/Token")
public class TokenResource extends AbstractTokenResource {

    public static final String CLIENT_ASSERTION_TYPE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";
    // This will be removed as part of DPC-747
    private static final String DEFAULT_ACCESS_SCOPE = "system/*.*";
    private static final Logger logger = LoggerFactory.getLogger(TokenResource.class);
    private static final String INVALID_JWT_MSG = "Invalid JWT";
    public static final String AUTH_URL_PATTERN = "%s/Token/auth";

    private final TokenDAO tokenDao;
    private final MacaroonBakery bakery;
    private final TokenPolicy policy;
    private final IJTICache cache;
    private final String authURL;
    
    private final JwtParser verificationParser;
    private final JwtParser authParser;


    @Inject
    public TokenResource(TokenDAO tokenDao,
                         PublicKeyDAO keyDao,
                         MacaroonBakery bakery,
                         TokenPolicy policy,
                         SigningKeyResolverAdapter resolver,
                         IJTICache cache,
                         @APIV1 String publicURL) {
        this.tokenDao = tokenDao;
        this.bakery = bakery;
        this.policy = policy;
        this.cache = cache;
        this.authURL = String.format(AUTH_URL_PATTERN, publicURL);

        verificationParser = Jwts.parser()
            .setSigningKeyResolver(new ValidatingKeyResolver(keyDao, cache, authURL))
            .requireAudience(this.authURL)
            .build();

        authParser = Jwts.parser()
            .setSigningKeyResolver(resolver)
            .requireAudience(this.authURL)
            .build();
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
        return new CollectionResponse<>(this.tokenDao.fetchTokens(organizationPrincipal.getID()));
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
        final List<TokenEntity> tokens = this.tokenDao.findTokenByOrgAndID(principal.getID(), tokenID);
        if (tokens.isEmpty()) {
            throw new NotFoundException("Cannot find token with matching ID");
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
            persisted = this.tokenDao.persistToken(tokenEntity);
        } catch (NoResultException e) {
            throw new NotFoundException(String.format("Cannot find Organization: %s", organizationID));
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
        final List<TokenEntity> matchedToken = this.tokenDao.findTokenByOrgAndID(organizationPrincipal.getID(), tokenID);
        assert matchedToken.size() == 1 : "Should only have a single matching token";

        this.tokenDao.deleteToken(matchedToken.get(0));

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
            @FormParam(value = "scope") String scope,
            @ApiParam(name = "grant_type", value = "Authorization grant type", required = true, allowableValues = "client_credentials")
            @FormParam(value = "grant_type") String grantType,
            @ApiParam(name = "client_assertion_type", value = "Client Assertion Type", required = true, allowableValues = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer")
            @FormParam(value = "client_assertion_type") String clientAssertionType,
            @ApiParam(name = "client_assertion", value = "Signed JWT", required = true)
            @FormParam(value = "client_assertion") String jwtBody) {
        // Actual scope implementation will come as part of DPC-747
        validateJWTQueryParams(grantType, clientAssertionType, scope, jwtBody);

        // Validate JWT signature
        try {
            return handleJWT(jwtBody, scope);
        } catch (SecurityException e) {
            logger.error("JWT has invalid signature", e);
            throw new NotAuthorizedException(INVALID_JWT_MSG, Response.status(Response.Status.UNAUTHORIZED).build());
        } catch (JwtException e) {
            logger.error("Malformed JWT", e);
            throw new NotAuthorizedException(INVALID_JWT_MSG, Response.status(Response.Status.UNAUTHORIZED).build());
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
            verificationParser.parse(jwt);
        } catch (NotDeSerializableException | UnsupportedJwtException | MalformedJwtException e) {
            throw new BadRequestException("JWT is not formatted, signed, or populated correctly");
        } catch(BadRequestException e) {
            throw e;
        }

        return Response.ok().build();
    }

    private void validateJWTQueryParams(String grantType, String clientAssertionType, String scope, String jwtBody) {
        if (!grantType.equals("client_credentials")) {
            throw new BadRequestException("Grant Type must be 'client_credentials'");
        }

        if (!clientAssertionType.equals(CLIENT_ASSERTION_TYPE)) {
            throw new BadRequestException(String.format("Client Assertion Type must be '%s'", CLIENT_ASSERTION_TYPE));
        }

        if (!scope.equals(DEFAULT_ACCESS_SCOPE)) {
            throw new BadRequestException(String.format("Access Scope must be '%s'", DEFAULT_ACCESS_SCOPE));
        }

        if (jwtBody == null || jwtBody.isEmpty()) {
            throw new BadRequestException("Client Assertion must be present");
        }
    }

    private JWTAuthResponse handleJWT(String jwtBody, String requestedScope) {
        Jws<Claims> claims = authParser.parseSignedClaims(jwtBody);

        // Extract the Client Macaroon from the subject field (which is the same as the issuer)
        final String clientMacaroon = claims.getPayload().getSubject();
        final List<Macaroon> macaroons = MacaroonBakery.deserializeMacaroon(clientMacaroon);

        // Get org id from macaroon caveats
        UUID orgId = MacaroonHelpers.extractOrgIDFromCaveats(macaroons).orElseThrow(() -> {
            logger.error("No organization found on macaroon");
            throw new NotAuthorizedException(INVALID_JWT_MSG, Response.status(Response.Status.UNAUTHORIZED));
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
                throw new BadRequestException("Cannot set expiration after policy default");
            }

            // Verify is not already expired
            if (customExpiration.isBefore(now)) {
                throw new BadRequestException("Cannot set expiration before current time");
            }
            return customExpiration;
        }
        return defaultExpiration;
    }

    @SuppressWarnings("JdkObsolete") // Date class is used by Jwt
    private void handleJWTClaims(UUID organizationID, Jws<Claims> claims) {
        // Issuer and Sub must be present and identical
        final String issuer = getClaimIfPresent("issuer", claims.getPayload().getIssuer());
        final String subject = getClaimIfPresent("subject", claims.getPayload().getSubject());
        if (!issuer.equals(subject)) {
            throw new BadRequestException("Issuer and Subject must be identical");
        }

        // JTI must be present and have not been used in the past 5 minutes.
        if (!this.cache.isJTIOk(getClaimIfPresent("id", claims.getPayload().getId()), true)) {
            logger.warn("JWT being replayed for organization {}", organizationID);
            throw new NotAuthorizedException(INVALID_JWT_MSG, Response.status(Response.Status.UNAUTHORIZED));
        }

        // Ensure the expiration time for the token is not more than 5 minutes in the future
        final Date expiration = getClaimIfPresent("expiration", claims.getPayload().getExpiration());
        if (OffsetDateTime.now(ZoneOffset.UTC).plus(5, ChronoUnit.MINUTES).isBefore(expiration.toInstant().atOffset(ZoneOffset.UTC))) {
            throw new NotAuthorizedException("Not authorized", Response.status(Response.Status.UNAUTHORIZED));
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
            throw new BadRequestException(String.format("Claim %s must be present", claimName));
        }
        return claim;
    }
}
