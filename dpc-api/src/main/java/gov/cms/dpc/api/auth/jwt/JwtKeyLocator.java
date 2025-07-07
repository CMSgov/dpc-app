package gov.cms.dpc.api.auth.jwt;

import gov.cms.dpc.api.entities.PublicKeyEntity;
import gov.cms.dpc.api.exceptions.PublicKeyException;
import gov.cms.dpc.api.jdbi.PublicKeyDAO;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.LocatorAdapter;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Key;
import java.util.UUID;

public class JwtKeyLocator extends LocatorAdapter<Key> {

    private static final Logger logger = LoggerFactory.getLogger(JwtKeyLocator.class);

    private final PublicKeyDAO dao;

    @Inject
    public JwtKeyLocator(PublicKeyDAO dao) {
        this.dao = dao;
    }

    @Override
    public Key locate(JwsHeader header) {
        final String keyId = header.getKeyId();
        if (keyId == null) {
            logger.error("JWT KID field is missing");
            throw new WebApplicationException("JWT must have KID field", Response.Status.UNAUTHORIZED);
        }

//        final UUID organizationID = getOrganizationID(header.get("iss"));
//        // Set the MDC values here, since it's the first time we actually know what the organization ID is
//        MDC.put(MDCConstants.ORGANIZATION_ID, organizationID.toString());

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
