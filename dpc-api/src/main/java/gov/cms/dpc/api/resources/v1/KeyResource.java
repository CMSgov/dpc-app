package gov.cms.dpc.api.resources.v1;


import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.auth.PublicKeyHandler;
import gov.cms.dpc.api.entities.PublicKeyEntity;
import gov.cms.dpc.api.jdbi.PublicKeyDAO;
import gov.cms.dpc.api.resources.AbstractKeyResource;
import gov.cms.dpc.common.entities.OrganizationEntity;
import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
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
import java.util.UUID;

@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Key", tags = {"Auth"})
public class KeyResource extends AbstractKeyResource {

    private static final Logger logger = LoggerFactory.getLogger(KeyResource.class);

    private final PublicKeyDAO dao;

    @Inject
    KeyResource(PublicKeyDAO dao) {
        this.dao = dao;
    }

    @GET
    @Timed
    @ExceptionMetered
    @UnitOfWork
    @Override
    public List<PublicKeyEntity> getPublicKeys(@ApiParam(hidden = true) @Auth OrganizationPrincipal organizationPrincipal) {
        return this.dao.fetchPublicKeys(organizationPrincipal.getID());
    }

    @GET
    @Timed
    @ExceptionMetered
    @Path("/{keyID}")
    @UnitOfWork
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
    @Override
    public Response deletePublicKey(@ApiParam(hidden = true) @Auth OrganizationPrincipal organizationPrincipal, @NotNull @PathParam(value = "keyID") UUID keyID) {
        this.dao.deletePublicKey(keyID, organizationPrincipal.getID());

        return Response.ok().build();
    }

    @POST
    @Timed
    @ExceptionMetered
    @Consumes(MediaType.TEXT_PLAIN)
    @UnitOfWork
    @Override
    public PublicKeyEntity submitKey(@ApiParam(hidden = true) @Auth OrganizationPrincipal organizationPrincipal, @NotEmpty String key) {
        final SubjectPublicKeyInfo publicKey = PublicKeyHandler.parsePEMString(key);

        final PublicKeyEntity publicKeyEntity = new PublicKeyEntity();
        final OrganizationEntity organizationEntity = new OrganizationEntity();
        organizationEntity.setId(organizationPrincipal.getID());

        publicKeyEntity.setManagingOrganization(organizationEntity);
        publicKeyEntity.setId(UUID.randomUUID());
        publicKeyEntity.setPublicKey(publicKey);

        return this.dao.persistPublicKey(publicKeyEntity);
    }
}
