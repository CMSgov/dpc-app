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
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.security.Key;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JwtKeyResolver extends SigningKeyResolverAdapter {

    private static final Logger logger = LoggerFactory.getLogger(JwtKeyResolver.class);

    private final PublicKeyDAO dao;

    @Inject
    public JwtKeyResolver(PublicKeyDAO dao) {
        this.dao = dao;
    }

    @Override
    @SuppressWarnings("rawtypes") // We need to suppress this because the Raw type is part of the signature we inherit
    public Key resolveSigningKey(JwsHeader header, Claims claims) {
        final String keyId = header.getKeyId();
        if (keyId == null) {
            logger.error("JWT KID field is missing");
            throw new WebApplicationException("JWT must have KID field", Response.Status.UNAUTHORIZED);
        }

        final PublicKeyEntity keyEntity;
        try {
            keyEntity = this.dao.fetchPublicKey(UUID.fromString(keyId))
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
}
