package gov.cms.dpc.api.resources;

import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.entities.PublicKeyEntity;
import gov.cms.dpc.api.models.CollectionResponse;
import gov.cms.dpc.api.resources.v1.KeyResource;

import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.util.Optional;
import java.util.UUID;

@Path("/Key")
public abstract class AbstractKeyResource {

    @GET
    public abstract CollectionResponse<PublicKeyEntity> getPublicKeys(OrganizationPrincipal organizationPrincipal);

    @GET
    @Path("/{keyID}")
    public abstract PublicKeyEntity getPublicKey(OrganizationPrincipal organizationPrincipal, @NotNull UUID keyID);

    @DELETE
    @Path("/{keyID}")
    public abstract Response deletePublicKey(OrganizationPrincipal organizationPrincipal, @NotNull UUID keyID);

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @POST
    public abstract PublicKeyEntity submitKey(OrganizationPrincipal organizationPrincipal, KeyResource.KeySignature keySignature, Optional<String> keyLabelOptional);
}
