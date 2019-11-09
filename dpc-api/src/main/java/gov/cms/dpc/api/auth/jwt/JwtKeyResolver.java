package gov.cms.dpc.api.auth.jwt;

import com.github.nitram509.jmacaroons.Macaroon;
import gov.cms.dpc.api.auth.MacaroonHelpers;
import gov.cms.dpc.api.entities.PublicKeyEntity;
import gov.cms.dpc.api.exceptions.PublicKeyException;
import gov.cms.dpc.api.jdbi.PublicKeyDAO;
import gov.cms.dpc.macaroons.MacaroonBakery;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.security.Key;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JwtKeyResolver extends SigningKeyResolverAdapter {

    private static final Logger logger = LoggerFactory.getLogger(JwtKeyResolver.class);

    private final PublicKeyDAO dao;
    private final MacaroonBakery bakery;

    @Inject
    public JwtKeyResolver(MacaroonBakery bakery, PublicKeyDAO dao) {
        this.dao = dao;
        this.bakery = bakery;
    }

    @Override
    @SuppressWarnings("rawtypes") // We need to suppress this because the Raw type is part of the signature we inherit
    public Key resolveSigningKey(JwsHeader header, Claims claims) {
        final UUID organizationID = getOrganizationIDFromClaims(claims)
                .orElseThrow(() -> {
                    logger.error("Cannot find organization_id on token");
                    throw new WebApplicationException("Token is invalid", Response.Status.UNAUTHORIZED);
                });
        final String keyId = header.getKeyId();
        if (keyId == null) {
            logger.error("JWT KID field is missing");
            throw new WebApplicationException("JWT must have KID field", Response.Status.UNAUTHORIZED);
        }

        final PublicKeyEntity keyByLabel;
        try {
            keyByLabel = this.dao.findKeyByLabel(organizationID, keyId);
        } catch (NoResultException e) {
            throw new WebApplicationException(String.format("Cannot find public key with label: %s", keyId), Response.Status.UNAUTHORIZED);
        }

        try {
            return PublicKeyHandler.publicKeyFromEntity(keyByLabel);
        } catch (PublicKeyException e) {
            logger.error("Cannot convert public key", e);
            throw new WebApplicationException("Internal server error", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    public Optional<UUID> getOrganizationIDFromClaims(Claims claims) {
        final List<Macaroon> macaroons = this.bakery.deserializeMacaroon(claims.getIssuer());
        return MacaroonHelpers.extractOrgIDFromCaveats(this.bakery, macaroons);
    }
}
