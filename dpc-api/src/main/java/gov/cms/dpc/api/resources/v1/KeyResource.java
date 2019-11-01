package gov.cms.dpc.api.resources.v1;


import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.auth.jwt.PublicKeyHandler;
import gov.cms.dpc.api.entities.PublicKeyEntity;
import gov.cms.dpc.api.exceptions.PublicKeyException;
import gov.cms.dpc.api.jdbi.PublicKeyDAO;
import gov.cms.dpc.api.resources.AbstractKeyResource;
import gov.cms.dpc.common.entities.OrganizationEntity;
import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import io.swagger.annotations.*;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Produces(MediaType.APPLICATION_JSON)
@Api(tags = {"Auth", "Key"}, authorizations = @Authorization(value = "apiKey"))
public class KeyResource extends AbstractKeyResource {

    private static final Logger logger = LoggerFactory.getLogger(KeyResource.class);

    private final PublicKeyDAO dao;

    @Inject
    public KeyResource(PublicKeyDAO dao) {
        this.dao = dao;
    }

    @GET
    @Timed
    @ExceptionMetered
    @UnitOfWork
    @ApiOperation(value = "Fetch public keys for Organization",
            notes = "This endpoint returns all the public keys currently associated with the organization." +
                    "<p>The returned keys are serialized using PEM encoding.",
            authorizations = @Authorization(value = "apiKey"))
    @Override
    public List<PublicKeyEntity> getPublicKeys(@ApiParam(hidden = true) @Auth OrganizationPrincipal organizationPrincipal) {
        return this.dao.fetchPublicKeys(organizationPrincipal.getID());
    }

    @GET
    @Timed
    @ExceptionMetered
    @Path("/{keyID}")
    @UnitOfWork
    @ApiOperation(value = "Fetch public key for Organization",
            notes = "This endpoint returns the specified public key associated with the organization." +
                    "<p>The returned keys are serialized using PEM encoding.")
    @ApiResponses(@ApiResponse(code = 404, message = "Cannot find public key for organization"))
    @Override
    public PublicKeyEntity getPublicKey(@ApiParam(hidden = true) @Auth OrganizationPrincipal organizationPrincipal, @NotNull @PathParam(value = "keyID") UUID keyID) {
        final List<PublicKeyEntity> certificates = this.dao.fetchPublicKey(keyID, organizationPrincipal.getID());
        if (certificates.isEmpty()) {
            throw new WebApplicationException("Cannot find certificate", Response.Status.NOT_FOUND);
        }
        return certificates.get(0);
    }

    @DELETE
    @Timed
    @ExceptionMetered
    @Path("/{keyID}")
    @UnitOfWork
    @ApiOperation(value = "Delete public key for Organization",
            notes = "This endpoint deletes the specified public key associated with the organization.")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Cannot find public key for organization"),
            @ApiResponse(code = 200, message = "Key successfully removed")
    })
    @Override
    public Response deletePublicKey(@ApiParam(hidden = true) @Auth OrganizationPrincipal organizationPrincipal, @NotNull @PathParam(value = "keyID") UUID keyID) {
        this.dao.deletePublicKey(keyID, organizationPrincipal.getID());

        return Response.ok().build();
    }

    @POST
    @Timed
    @ExceptionMetered
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Register public key for Organization",
            notes = "This endpoint registers the provided public key with the organization." +
                    "<p>The provided key MUST be PEM encoded.")
    @ApiResponses(@ApiResponse(code = 400, message = "Public key is not valid."))
    @UnitOfWork
    @Override
    public PublicKeyEntity submitKey(@ApiParam(hidden = true) @Auth OrganizationPrincipal organizationPrincipal,
                                     @ApiParam(example = "---PUBLIC KEY---......---END PUBLIC KEY---")
                                     @NotEmpty String key,
                                     @ApiParam(name = "label", value = "Public Key ID (label)", defaultValue = "key:{count of public keys + 1) ")
                                     @QueryParam(value = "label") Optional<String> keyID) {
        final String keyLabel = keyID.orElseGet(() -> this.buildDefaultKeyID(organizationPrincipal.getID()));
        final SubjectPublicKeyInfo publicKey;
        try {
            publicKey = PublicKeyHandler.parsePEMString(key);
        } catch (PublicKeyException e) {
            logger.error("Cannot parse provided public key.", e);
            throw new WebApplicationException("Public key is not valid", Response.Status.BAD_REQUEST);
        }

        final PublicKeyEntity publicKeyEntity = new PublicKeyEntity();
        final OrganizationEntity organizationEntity = new OrganizationEntity();
        organizationEntity.setId(organizationPrincipal.getID());

        publicKeyEntity.setOrganization_id(organizationEntity.getId());
        publicKeyEntity.setId(UUID.randomUUID());
        publicKeyEntity.setPublicKey(publicKey);
        publicKeyEntity.setLabel(keyLabel);

        return this.dao.persistPublicKey(publicKeyEntity);
    }

    private String buildDefaultKeyID(UUID organizationID) {
        final int newKeyID = this.dao.fetchPublicKeys(organizationID).size() + 1;
        return String.format("key:%d", newKeyID);
    }
}
