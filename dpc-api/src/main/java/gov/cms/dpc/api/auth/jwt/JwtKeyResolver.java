package gov.cms.dpc.api.auth.jwt;

import com.github.nitram509.jmacaroons.Macaroon;
import gov.cms.dpc.api.entities.PublicKeyEntity;
import gov.cms.dpc.api.exceptions.PublicKeyException;
import gov.cms.dpc.api.jdbi.PublicKeyDAO;
import gov.cms.dpc.common.MDCConstants;
import gov.cms.dpc.macaroons.MacaroonBakery;
import gov.cms.dpc.macaroons.MacaroonCaveat;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.security.Key;
import java.util.List;
import java.util.UUID;

import static gov.cms.dpc.api.auth.MacaroonHelpers.ORGANIZATION_CAVEAT_KEY;

public class JwtKeyResolver extends SigningKeyResolverAdapter {

    private static final Logger logger = LoggerFactory.getLogger(JwtKeyResolver.class);

    private final PublicKeyDAO dao;

    @Inject
    public JwtKeyResolver(PublicKeyDAO dao) {
        this.dao = dao;
    }

    @Override
    // We need to suppress this because the Raw type is part of the signature we inherit
    public Key resolveSigningKey(JwsHeader header, Claims claims) {
        final String keyId = header.getKeyId();
        if (keyId == null) {
            logger.error("JWT KID field is missing");
            throw new WebApplicationException("JWT must have KID field", Response.Status.UNAUTHORIZED);
        }

        final UUID organizationID = getOrganizationID(claims.getIssuer());
        // Set the MDC values here, since it's the first time we actually know what the organization ID is
        MDC.put(MDCConstants.ORGANIZATION_ID, organizationID.toString());

        final PublicKeyEntity keyEntity;
        try {
            keyEntity = this.dao.fetchPublicKey(organizationID, UUID.fromString(keyId))
                    .orElseThrow(() -> new WebApplicationException(String.format("Cannot find public key with id: %s", keyId), Response.Status.UNAUTHORIZED));
        } catch (IllegalArgumentException e) {
            logger.error("Cannot convert '{}' to UUID", keyId, e);
            throw new WebApplicationException("Invalid Public Key ID", Response.Status.UNAUTHORIZED);
        }

        try {
            return PublicKeyHandler.publicKeyFromEntity(keyEntity);
        } catch (PublicKeyException e) {
            logger.error("Cannot convert public key", e);
            throw new WebApplicationException("Internal server error", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    protected UUID getOrganizationID(String macaroon) {
        if (macaroon == null || macaroon.isEmpty()) {
            throw new WebApplicationException("JWT must have client_id", Response.Status.UNAUTHORIZED);
        }
        final List<Macaroon> macaroons = MacaroonBakery.deserializeMacaroon(macaroon);
        if (macaroons.isEmpty()) {
            throw new WebApplicationException("JWT must have client_id", Response.Status.UNAUTHORIZED);
        }

        return MacaroonBakery.getCaveats(macaroons.get(0))
                .stream()
                .map(MacaroonCaveat::getCondition)
                .filter(cond -> cond.getKey().equals(ORGANIZATION_CAVEAT_KEY))
                .map(condition -> UUID.fromString(condition.getValue()))
                .findAny()
                .orElseThrow(() -> new WebApplicationException("JWT client token must have organization_id", Response.Status.UNAUTHORIZED));
    }
}
