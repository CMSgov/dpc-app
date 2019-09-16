package gov.cms.dpc.api.resources.v1;


import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.jdbi.CertificateDAO;
import gov.cms.dpc.api.resources.AbstractCertificateResource;
import gov.cms.dpc.api.entities.CertificateEntity;
import gov.cms.dpc.common.entities.OrganizationEntity;
import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Certificate", tags = {"Auth"})
public class CertificateResource extends AbstractCertificateResource {

    private static final Logger logger = LoggerFactory.getLogger(CertificateResource.class);

    private final Base64.Decoder decoder;
    private final Base64.Encoder encoder;
    private final CertificateDAO dao;

    @Inject
    CertificateResource(CertificateDAO dao) {
        this.dao = dao;
        this.decoder = Base64.getUrlDecoder();
        this.encoder = Base64.getUrlEncoder();
    }

    @GET
    @Timed
    @ExceptionMetered
    @Path("/{certificateID}")
    @UnitOfWork
    @Override
    public CertificateEntity getCertificate(@ApiParam(hidden = true) @Auth OrganizationPrincipal organizationPrincipal, @PathParam(value = "certificateID") UUID certificateID) {
        final List<CertificateEntity> certificates = this.dao.fetchCertificate(certificateID, organizationPrincipal.getID());
        if (certificates.isEmpty()) {
            throw new WebApplicationException("Cannot find certificate", Response.Status.NOT_FOUND);
        }

        return certificates.get(0);
    }

    @POST
    @Timed
    @ExceptionMetered
    @Consumes(MediaType.TEXT_PLAIN)
    @UnitOfWork
    @Override
    public CertificateEntity submitCertificate(@ApiParam(hidden = true) @Auth OrganizationPrincipal organizationPrincipal, String certificate) {
        final SubjectPublicKeyInfo publicKey;
        final ByteArrayInputStream bas = new ByteArrayInputStream(certificate.getBytes(StandardCharsets.ISO_8859_1));
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(bas))) {
            try (PEMParser pemParser = new PEMParser(bufferedReader)) {
                final Object object = pemParser.readObject();
                if (!(object instanceof SubjectPublicKeyInfo)) {
                    logger.error("Cannot convert {} to {}.", object.getClass().getName(), SubjectPublicKeyInfo.class.getName());
                    throw new WebApplicationException("Must submit public key", Response.Status.BAD_REQUEST);
                }
                publicKey = (SubjectPublicKeyInfo) object;
            }
        } catch (IOException e) {
            logger.error("Unable to read Certificate input", e);
            throw new WebApplicationException("Cannot read Certificate input", Response.Status.BAD_REQUEST);
        }

        // Validate Certificate
        final CertificateEntity certificateEntity = new CertificateEntity();
        final OrganizationEntity organizationEntity = new OrganizationEntity();
        organizationEntity.setId(organizationPrincipal.getID());

        certificateEntity.setManagingOrganization(organizationEntity);
        certificateEntity.setId(UUID.randomUUID());
        certificateEntity.setCertificate(publicKey);

        return this.dao.persistCertificate(certificateEntity);
    }
}
