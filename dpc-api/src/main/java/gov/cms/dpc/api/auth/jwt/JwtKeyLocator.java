package gov.cms.dpc.api.auth.jwt;

import gov.cms.dpc.api.entities.PublicKeyEntity;
import gov.cms.dpc.api.exceptions.PublicKeyException;
import gov.cms.dpc.api.jdbi.PublicKeyDAO;
import io.jsonwebtoken.JwsHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import java.security.Key;
import java.util.UUID;

import io.jsonwebtoken.LocatorAdapter;
import jakarta.inject.Singleton;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.Response;

public class JwtKeyLocator extends LocatorAdapter<Key> {

    private static final Logger logger = LoggerFactory.getLogger(JwtKeyLocator.class);

    private final PublicKeyDAO dao;

    @Inject
    @Singleton
    public JwtKeyLocator(PublicKeyDAO dao) {
        this.dao = dao;
    }

    @Override
    protected Key locate(JwsHeader header) {
        final String keyId = header.getKeyId();
        if (keyId == null) {
            logger.error("JWT KID field is missing");
            throw new NotAuthorizedException("JWT header must have `kid` value", Response.status(Response.Status.UNAUTHORIZED));
        }

        final PublicKeyEntity keyEntity;
        try {
            keyEntity = this.dao.fetchPublicKey(UUID.fromString(keyId))
                    .orElseThrow(() -> new NotAuthorizedException(String.format("Cannot find public key with id: %s", keyId), Response.status(Response.Status.UNAUTHORIZED)));
        } catch (IllegalArgumentException e) {
            logger.error("Cannot convert '{}' to UUID", keyId, e);
            throw new BadRequestException("`kid` value must be a UUID");
        }

        try {
            return PublicKeyHandler.publicKeyFromEntity(keyEntity);
        } catch (PublicKeyException e) {
            logger.error("Cannot convert public key", e);
            throw new InternalServerErrorException("Internal server error");
        }
    }
}
