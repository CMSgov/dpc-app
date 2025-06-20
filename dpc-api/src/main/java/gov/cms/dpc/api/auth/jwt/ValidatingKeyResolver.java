package gov.cms.dpc.api.auth.jwt;

import gov.cms.dpc.macaroons.MacaroonBakery;
import gov.cms.dpc.macaroons.exceptions.BakeryException;
import jakarta.annotation.Nullable;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.jwx.Headers;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Simply verifies whether the required claims and values are present.
 */
public class ValidatingKeyResolver {

    private final IJTICache cache;
    private final List<String> audClaim;

    public ValidatingKeyResolver(IJTICache cache, List<String> audClaim) {
        this.cache = cache;
        this.audClaim = audClaim;
    }

    public void resolveSigningKey(JwtContext jwtContext) throws MalformedClaimException {
        Headers headers = jwtContext.getJoseObjects().get(0).getHeaders();
        JwtClaims claims = jwtContext.getJwtClaims();

        validateHeader(headers);
        validateExpiration(claims);
        final String issuer = getClaimIfPresent("issuer", claims.getIssuer());
        validateClaims(issuer, claims);
        validateTokenFormat(issuer);
    }

    void validateHeader(Headers headers) {
        final String keyId = headers.getStringHeaderValue("kid");
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

    void validateExpiration(JwtClaims claims) throws MalformedClaimException {
        // Verify not expired and not more than 5 minutes in the future
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        final OffsetDateTime expiration;
        try {
            final int epochSeconds = Math.toIntExact(getClaimIfPresent("expiration", claims.getExpirationTime()).getValue());
            expiration = Instant.ofEpochSecond(epochSeconds).atOffset(ZoneOffset.UTC);
        } catch (ArithmeticException e) {
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

    void validateClaims(String issuer, JwtClaims claims) throws MalformedClaimException {
        // JTI must be present and have not been used in the past 5 minutes.
        if (!this.cache.isJTIOk(getClaimIfPresent("id", claims.getJwtId()), false)) {
            throw new WebApplicationException("Token ID cannot be re-used", Response.Status.BAD_REQUEST);
        }

        // Issuer and Sub match
        final String subject = getClaimIfPresent("subject", claims.getSubject());

        if (!issuer.equals(subject)) {
            throw new WebApplicationException("Issuer and Subject must be identical", Response.Status.BAD_REQUEST);
        }

        // Test correct aud claim
        final List<String> audience = getClaimIfPresent("audience", claims.getAudience());
        if (!audience.equals(this.audClaim)) {
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
