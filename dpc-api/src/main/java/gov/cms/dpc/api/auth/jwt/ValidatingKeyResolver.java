package gov.cms.dpc.api.auth.jwt;

import gov.cms.dpc.macaroons.MacaroonBakery;
import gov.cms.dpc.macaroons.exceptions.BakeryException;
import io.jsonwebtoken.*;
import jakarta.annotation.Nullable;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.security.Key;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

/**
 * Implementation of {@link SigningKeyResolverAdapter} that simply verifies whether the required claims and values are present.
 * As far as I can tell, this is the only way to get access to the JWS claims without actually verifying the signature.
 * See: https://github.com/jwtk/jjwt/issues/205
 * <p>
 * The downside is that this method will always return a null {@link Key}, which means the {@link Jwts#parser()} method will always throw an {@link UnsupportedJwtException}, which we need to catch.
 */
public class ValidatingKeyResolver extends SigningKeyResolverAdapter {

    private final IJTICache cache;
    private final Set<String> audClaim;

    public ValidatingKeyResolver(IJTICache cache, Set<String> audClaim) {
        this.cache = cache;
        this.audClaim = audClaim;
    }

    @Override
    public Key resolveSigningKey(JwsHeader header, Claims claims) {
        validateHeader(header);
        validateExpiration(claims);
        final String issuer = getClaimIfPresent("issuer", claims.getIssuer());
        validateClaims(issuer, claims);
        validateTokenFormat(issuer);
        return null;
    }

    void validateHeader(JwsHeader header) {
        final String keyId = header.getKeyId();
        if (keyId == null) {
            throw new WebApplicationException("JWT header must have `kid` value", Response.Status.BAD_REQUEST);
        }

        // Make sure it's a UUID
        try {
            UUID.fromString(keyId);
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException("`kid` value must be a UUID", Response.Status.BAD_REQUEST);
        }
    }

    void validateTokenFormat(String issuer) {
        // Make sure the client token is actually a macaroon and not something else, like a UUID
        try {
            UUID.fromString(issuer);
            throw new WebApplicationException("Cannot use Token ID as `client_token`, must use actual token value", Response.Status.BAD_REQUEST);
        } catch (IllegalArgumentException e) {
            // If the parsing fails, then we know it's not a UUID, which it shouldn't be, so continue
        }

        try {
            MacaroonBakery.deserializeMacaroon(issuer);
        } catch (BakeryException e) {
            throw new WebApplicationException("Client token is not formatted correctly", Response.Status.BAD_REQUEST);
        }
    }

    void validateExpiration(Claims claims) {
        // Verify not expired and not more than 5 minutes in the future
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        final OffsetDateTime expiration;
        try {
            final Integer epochSeconds = getClaimIfPresent("expiration", claims.get("exp", Integer.class));
            expiration = Instant.ofEpochSecond(epochSeconds).atOffset(ZoneOffset.UTC);
        } catch (RequiredTypeException e) {
            throw new WebApplicationException("Expiration time must be seconds since unix epoch", Response.Status.BAD_REQUEST);
        }

        // ensure Not expired
        if (now.isAfter(expiration)) {
            throw new WebApplicationException("JWT is expired", Response.Status.BAD_REQUEST);
        }

        // Not more than 5 minutes in the future
        if (now.plusMinutes(5).isBefore(expiration.toInstant().atOffset(ZoneOffset.UTC))) {
            throw new WebApplicationException("Token expiration cannot be more than 5 minutes in the future", Response.Status.BAD_REQUEST);
        }
    }

    void validateClaims(String issuer, Claims claims) {
        // JTI must be present and have not been used in the past 5 minutes.
        if (!this.cache.isJTIOk(getClaimIfPresent("id", claims.getId()), false)) {
            throw new WebApplicationException("Token ID cannot be re-used", Response.Status.BAD_REQUEST);
        }

        // Issuer and Sub match
        final String subject = getClaimIfPresent("subject", claims.getSubject());

        if (!issuer.equals(subject)) {
            throw new WebApplicationException("Issuer and Subject must be identical", Response.Status.BAD_REQUEST);
        }

        // Test correct aud claim
        final Set<String> audience = getClaimIfPresent("audience", claims.getAudience());
        if (!audience.equals(this.audClaim)) {
            System.out.println("AUDIENCE: " + audience);
            System.out.println("AUD CLAIM: " + this.audClaim);
            throw new WebApplicationException("Audience claim value is incorrect", Response.Status.BAD_REQUEST);
        }
    }

    private static <T> T getClaimIfPresent(String claimName, @Nullable T claim) {
        if (claim == null) {
            throw new WebApplicationException(String.format("Claim `%s` must be present", claimName), Response.Status.BAD_REQUEST);
        }
        return claim;
    }
}
