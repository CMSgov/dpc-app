package gov.cms.dpc.api.auth.jwt;

import com.github.nitram509.jmacaroons.Macaroon;
import gov.cms.dpc.api.auth.MacaroonHelpers;
import gov.cms.dpc.api.entities.PublicKeyEntity;
import gov.cms.dpc.api.exceptions.PublicKeyException;
import gov.cms.dpc.api.jdbi.PublicKeyDAO;
import gov.cms.dpc.macaroons.MacaroonBakery;
import gov.cms.dpc.macaroons.exceptions.BakeryException;
import io.jsonwebtoken.*;

import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.Response;
import java.security.Key;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

/**
 * Implementation of {@link SigningKeyResolverAdapter} that simply verifies whether or not the required claims and values are present.
 * As far as I can tell, this is the only way to get access to the JWS claims without actually verifying the signature.
 * See: https://github.com/jwtk/jjwt/issues/205
 * <p>
 * The downside is that this method will always return a null {@link Key}, which means the {@link Jwts#parser()} method will always throw an {@link IllegalArgumentException}, which we need to catch.
 */
@SuppressWarnings("rawtypes") // The JwsHeader comes as a generic, which bothers ErrorProne
public class ValidatingKeyResolver extends SigningKeyResolverAdapter {

    private final PublicKeyDAO dao;
    private final IJTICache cache;
    private final String audClaim;

    @Inject
    public ValidatingKeyResolver(PublicKeyDAO dao, IJTICache cache, String audClaim)
    {
        this.dao = dao;
        this.cache = cache;
        this.audClaim = audClaim;
    }

    @Override
    public Key resolveSigningKey(JwsHeader header, Claims claims) {
        validateHeader(header);
        validateExpiration(claims);
        final String issuer = getClaimIfPresent("issuer", claims.getIssuer());

        final String clientMacaroon = claims.getSubject();
        List<Macaroon> macaroons = null;
        try {
            macaroons = MacaroonBakery.deserializeMacaroon(clientMacaroon);
        }
        catch(BakeryException e) {
            return null;
        }

        // Get org id from macaroon caveats
        UUID orgId = MacaroonHelpers.extractOrgIDFromCaveats(macaroons).orElse(UUID.randomUUID());
            
        Key key = null;
        // if JWT has the key reference, get it
        try {
            PublicKeyEntity entity = dao.fetchPublicKey(orgId, UUID.fromString(header.getKeyId())).orElseThrow();
            key = PublicKeyHandler.publicKeyFromEntity(entity);
        } catch(PublicKeyException | NoSuchElementException e) {
            throw new NotAuthorizedException("Cannot find public key", Response.status(Response.Status.UNAUTHORIZED));
        }

        validateClaims(issuer, claims);
        validateTokenFormat(issuer);

        return key;
    }

    void validateHeader(JwsHeader header) {
        final String keyId = header.getKeyId();
        if (keyId == null) {
            throw new BadRequestException("JWT header must have `kid` value");
        }

        // Make sure it's a UUID
        try {
            //noinspection ResultOfMethodCallIgnored
            UUID.fromString(keyId);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("`kid` value must be a UUID");
        }
    }

    void validateTokenFormat(String issuer) {
        // Make sure the client token is actually a macaroon and not something else, like a a UUID
        try {
            //noinspection ResultOfMethodCallIgnored
            UUID.fromString(issuer);
            throw new BadRequestException("Cannot use Token ID as `client_token`, must use actual token value");
        } catch (IllegalArgumentException e) {
            // If the parsing fails, then we know it's not a UUID, which it shouldn't be, so continue
        }

        try {
            MacaroonBakery.deserializeMacaroon(issuer);
        } catch (BakeryException e) {
            throw new BadRequestException("Client token is not formatted correctly");
        }
    }

    void validateExpiration(Claims claims) {
        // Verify not expired and not more than 5 minutes in the future
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        final OffsetDateTime expiration;
        try {
            final Integer epochSeconds = getClaimIfPresent("expiration", claims.get("exp", Integer.class));
            expiration = Instant.ofEpochSecond(epochSeconds).atOffset(ZoneOffset.UTC);
        } catch (BadRequestException | RequiredTypeException e) {
            throw new BadRequestException("Expiration time must be seconds since unix epoch");
        }

        // ensure Not expired
        if (now.isAfter(expiration)) {
            throw new BadRequestException("JWT is expired");
        }

        // Not more than 5 minutes in the future
        if (now.plus(5, ChronoUnit.MINUTES).isBefore(expiration.toInstant().atOffset(ZoneOffset.UTC))) {
            throw new BadRequestException("Token expiration cannot be more than 5 minutes in the future");
        }
    }

    void validateClaims(String issuer, Claims claims) {
        // JTI must be present and have not been used in the past 5 minutes.
        if (!this.cache.isJTIOk(getClaimIfPresent("id", claims.getId()), false)) {
            throw new BadRequestException("Token ID cannot be re-used");
        }

        // Issuer and Sub match
        final String subject = getClaimIfPresent("subject", claims.getSubject());

        if (!issuer.equals(subject)) {
            throw new BadRequestException("Issuer and Subject must be identical");
        }

        // Test correct aud claim
        final Set<String> audience = getClaimIfPresent("audience", claims.getAudience());
        if (!audience.contains(this.audClaim)) {
            throw new BadRequestException("Audience claim value is incorrect");
        }
    }

    private static <T> T getClaimIfPresent(String claimName, @Nullable T claim) throws
            RequiredTypeException {
        if (claim == null) {
            throw new BadRequestException(String.format("Claim `%s` must be present", claimName));
        }
        return claim;
    }
}
