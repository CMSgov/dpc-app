package gov.cms.dpc.api.resources.v1;


import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.auth.annotations.Authorizer;
import gov.cms.dpc.api.auth.jwt.PublicKeyHandler;
import gov.cms.dpc.api.entities.PublicKeyEntity;
import gov.cms.dpc.api.exceptions.PublicKeyException;
import gov.cms.dpc.api.jdbi.PublicKeyDAO;
import gov.cms.dpc.api.models.CollectionResponse;
import gov.cms.dpc.api.resources.AbstractKeyResource;
import gov.cms.dpc.common.annotations.NoHtml;
import gov.cms.dpc.common.entities.OrganizationEntity;
import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import io.swagger.annotations.*;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Api(tags = {"Auth", "Key"}, authorizations = @Authorization(value = "access_token"))
@Path("/v1/Key")
public class KeyResource extends AbstractKeyResource {

    public static final String SNIPPET = "This is the snippet used to verify a key pair in DPC.";
    private static final Logger logger = LoggerFactory.getLogger(KeyResource.class);

    private final PublicKeyDAO dao;
    private final SecureRandom random;

    @Inject
    public KeyResource(PublicKeyDAO dao) {
        this.dao = dao;
        this.random = new SecureRandom();
    }

    @GET
    @Timed
    @ExceptionMetered
    @Authorizer
    @UnitOfWork("hibernate.auth")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Fetch public keys for Organization",
            notes = "This endpoint returns all the public keys currently associated with the organization." +
                    "<p>The returned keys are serialized using PEM encoding.",
            authorizations = @Authorization(value = "access_token"))
    @Override
    public CollectionResponse<PublicKeyEntity> getPublicKeys(@ApiParam(hidden = true) @Auth OrganizationPrincipal organizationPrincipal) {
        return new CollectionResponse<>(this.dao.fetchPublicKeys(organizationPrincipal.getID()));
    }

    @GET
    @Timed
    @ExceptionMetered
    @Path("/{keyID}")
    @UnitOfWork("hibernate.auth")
    @Authorizer
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Fetch public key for Organization",
            notes = "This endpoint returns the specified public key associated with the organization." +
                    "<p>The returned keys are serialized using PEM encoding.")
    @ApiResponses(@ApiResponse(code = 404, message = "Cannot find public key for organization"))
    @Override
    public PublicKeyEntity getPublicKey(@ApiParam(hidden = true) @Auth OrganizationPrincipal organizationPrincipal, @NotNull @PathParam(value = "keyID") UUID keyID) {
        final List<PublicKeyEntity> publicKeys = this.dao.publicKeySearch(keyID, organizationPrincipal.getID());
        if (publicKeys.isEmpty()) {
            throw new NotFoundException("Cannot find public key");
        }
        return publicKeys.get(0);
    }

    @DELETE
    @Timed
    @ExceptionMetered
    @Path("/{keyID}")
    @UnitOfWork("hibernate.auth")
    @Produces(MediaType.APPLICATION_JSON)
    @Authorizer
    @ApiOperation(value = "Delete public key for Organization",
            notes = "This endpoint deletes the specified public key associated with the organization.")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Cannot find public key for organization"),
            @ApiResponse(code = 200, message = "Key successfully removed")
    })
    @Override
    public Response deletePublicKey(@ApiParam(hidden = true) @Auth OrganizationPrincipal organizationPrincipal, @NotNull @PathParam(value = "keyID") UUID keyID) {
        final List<PublicKeyEntity> keys = this.dao.publicKeySearch(keyID, organizationPrincipal.getID());

        if (keys.isEmpty()) {
            throw new NotFoundException("Cannot find certificate");
        }
        keys.forEach(this.dao::deletePublicKey);

        return Response.ok().build();
    }

    @POST
    @Timed
    @ExceptionMetered
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Authorizer
    @ApiOperation(value = "Register public key for Organization",
            notes = "This endpoint registers the provided public key with the organization." +
                    "<p>The provided key MUST be PEM encoded." +
                    "<p>RSA keys of 4096-bits or greater are supported, as well as ECC keys using one of the following curves:" +
                    "- secp256r1" +
                    "- secp384r1")
    @ApiResponses(@ApiResponse(code = 400, message = "Public key is not valid."))
    @UnitOfWork("hibernate.auth")
    @Override
    public PublicKeyEntity submitKey(@ApiParam(hidden = true) @Auth OrganizationPrincipal organizationPrincipal,
                                     @ApiParam KeySignature keySignature,
                                     @ApiParam(name = "label", value = "Public Key Label (cannot be more than 25 characters in length)", defaultValue = "key:{random integer}", allowableValues = "range[-infinity, 25]")
                                     @QueryParam(value = "label") Optional<String> keyLabelOptional) {
        final String keyLabel;
        if (keyLabelOptional.isPresent()) {
            if (keyLabelOptional.get().length() > 25) {
                throw new BadRequestException("Key label cannot be more than 25 characters");
            }
            keyLabel = keyLabelOptional.get();
        } else {
            keyLabel = this.buildDefaultKeyID();
        }

        final String key = keySignature.getKey();
        final String signature = keySignature.getSignature();
        final SubjectPublicKeyInfo publicKey = parseAndValidateKey(key, signature);

        return savePublicKeyEntry(organizationPrincipal, keyLabel, publicKey);
    }

    private SubjectPublicKeyInfo parseAndValidateKey(String publicKeyPem, String sigStr) {
        final SubjectPublicKeyInfo publicKeyInfo;
        try {
            publicKeyInfo = PublicKeyHandler.parsePEMString(publicKeyPem);
        } catch (PublicKeyException e) {
            logger.error("Cannot parse provided public key.", e);
            throw new BadRequestException("Public key could not be parsed");
        }

        if (PublicKeyHandler.ECC_KEY.equals(publicKeyInfo.getAlgorithm().getAlgorithm())) {
            throw new UnprocessableEntityException("ECC keys are not currently supported");
        }

        // Validate public key
        try {
            PublicKeyHandler.validatePublicKey(publicKeyInfo);
        } catch (PublicKeyException e) {
            logger.error("Cannot validate provided public key.", e);
            throw new BadRequestException("Public key is not valid");
        }

        try {
            PublicKeyHandler.verifySignature(publicKeyPem, SNIPPET, sigStr);
        } catch (PublicKeyException e) {
            logger.error("Cannot verify signature with public key", e);
            throw new BadRequestException("Cannot verify signature with public key");
        }

        return publicKeyInfo;
    }

    private PublicKeyEntity savePublicKeyEntry(OrganizationPrincipal organizationPrincipal, String keyLabel, SubjectPublicKeyInfo publicKey) {
        final PublicKeyEntity publicKeyEntity = new PublicKeyEntity();
        final OrganizationEntity organizationEntity = new OrganizationEntity();
        organizationEntity.setId(organizationPrincipal.getID());

        publicKeyEntity.setOrganization_id(organizationEntity.getId());
        publicKeyEntity.setId(UUID.randomUUID());
        publicKeyEntity.setPublicKey(publicKey);
        publicKeyEntity.setLabel(keyLabel);
        
        try{
            return this.dao.persistPublicKey(publicKeyEntity);
        } catch(Exception e){
            logger.error("Exception saving public key: " + e);
            System.err.println("Exception saving public key: " + e);
            e.printStackTrace();
            throw new BadRequestException("Key cannot be re-used");
        }
    }

    private String buildDefaultKeyID() {
        final int newKeyID = this.random.nextInt();
        return String.format("key:%d", newKeyID);
    }

    public static class KeySignature {
        @NoHtml
        @NotEmpty
        private String key;
        @NoHtml
        @NotEmpty
        private String signature;

        public KeySignature() {}

        public KeySignature(String key, String signature) {
            this.key = key;
            this.signature = signature;
        }

        public String getKey() {
            return key;
        }

        public String getSignature() {
            return signature;
        }
    }
}
