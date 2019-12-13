package gov.cms.dpc.api.auth.jwt;

import com.github.nitram509.jmacaroons.MacaroonsBuilder;
import com.github.nitram509.jmacaroons.NotDeSerializableException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.SigningKeyResolverAdapter;

import javax.annotation.Nullable;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.security.Key;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class ValidatingKeyResolver extends SigningKeyResolverAdapter {

    private final IJTICache cache;
    private final String audClaim;

    public ValidatingKeyResolver(IJTICache cache, String audClaim) {
        this.cache = cache;
        this.audClaim = audClaim;
    }

    @Override
    public Key resolveSigningKey(JwsHeader header, Claims claims) {
        // Verify not expired and not more than 5 minutes in the future
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        final OffsetDateTime expiration = getClaimIfPresent("expiration", claims.getExpiration())
                .toInstant().atOffset(ZoneOffset.UTC);

        // ensure Not expired
        if (now.isAfter(expiration)) {
            throw new WebApplicationException("JWT is expired", Response.Status.BAD_REQUEST);
        }

        // Not more than 5 minutes in the future
        if (now.plus(5, ChronoUnit.MINUTES).isBefore(expiration.toInstant().atOffset(ZoneOffset.UTC))) {
            throw new WebApplicationException("Token expiration cannot be more than 5 minutes in the future", Response.Status.BAD_REQUEST);
        }

        // JTI must be present and have not been used in the past 5 minutes.
        if (!this.cache.isJTIOk(getClaimIfPresent("id", claims.getId()))) {
            throw new WebApplicationException("Token ID cannot be re-used", Response.Status.BAD_REQUEST);
        }

        // Issuer and Sub match
        final String issuer = getClaimIfPresent("issuer", claims.getIssuer());
        final String subject = getClaimIfPresent("subject", claims.getSubject());

        if (!issuer.equals(subject)) {
            throw new WebApplicationException("Issuer and Subject must be identical", Response.Status.BAD_REQUEST);
        }

        // Make sure the client token is actually a macaroon and not something else, like a a UUID
        try {
            //noinspection ResultOfMethodCallIgnored
            UUID.fromString(issuer);
            throw new WebApplicationException("Cannot use Token ID as `client_token`, must use actual token value", Response.Status.BAD_REQUEST);
        } catch (IllegalArgumentException e) {
            // If the parsing fails, then we know it's not a UUID, which it shouldn't be, so continue
        }

        try {
            MacaroonsBuilder.deserialize(issuer);
        } catch (NotDeSerializableException e) {
            throw new WebApplicationException("Client token is not formatted correctly", Response.Status.BAD_REQUEST);
        }

        // Test correct aud claim
        final String audience = getClaimIfPresent("audience", claims.getAudience());
        if (!audience.equals(this.audClaim)) {
            throw new WebApplicationException("Audience claim value is incorrect", Response.Status.BAD_REQUEST);
        }


        return null;
    }

    private static <T> T getClaimIfPresent(String claimName, @Nullable T claim) {
        if (claim == null) {
            throw new WebApplicationException(String.format("Claim `%s` must be present", claimName), Response.Status.BAD_REQUEST);
        }
        return claim;
    }
}
