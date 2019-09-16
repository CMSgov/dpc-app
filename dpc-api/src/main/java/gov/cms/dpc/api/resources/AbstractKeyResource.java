package gov.cms.dpc.api.resources;

import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.entities.PublicKeyEntity;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.util.UUID;

@Path("/Key")
public abstract class AbstractKeyResource {

    @GET
    @Path("/{keyID}")
    public abstract PublicKeyEntity getPublicKey(OrganizationPrincipal organizationPrincipal, UUID keyID);

    @POST
    public abstract PublicKeyEntity submitKey(OrganizationPrincipal organizationPrincipal, String key);
}
