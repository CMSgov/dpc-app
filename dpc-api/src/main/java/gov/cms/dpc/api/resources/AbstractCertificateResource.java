package gov.cms.dpc.api.resources;

import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.entities.CertificateEntity;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.util.UUID;

@Path("/Certificate")
public abstract class AbstractCertificateResource {

    @GET
    @Path("/{organizationID}")
    public abstract CertificateEntity getCertificate(OrganizationPrincipal organizationPrincipal, UUID certificateID);

    @POST
    public abstract CertificateEntity submitCertificate(OrganizationPrincipal organizationPrincipal, String certificate);
}
