package gov.cms.dpc.api.resources;

import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.entities.PublicKeyEntity;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

@Path("/Key")
public abstract class AbstractKeyResource {

    @GET
    public abstract List<PublicKeyEntity> getPublicKeys(OrganizationPrincipal organizationPrincipal);

    @GET
    @Path("/{keyID}")
    public abstract PublicKeyEntity getPublicKey(OrganizationPrincipal organizationPrincipal, @NotNull UUID keyID);

    @DELETE
    @Path("/{keyID}")
    public abstract Response deletePublicKey(OrganizationPrincipal organizationPrincipal, @NotNull UUID keyID);

    @POST
    public abstract PublicKeyEntity submitKey(OrganizationPrincipal organizationPrincipal, @NotEmpty String key);
}
