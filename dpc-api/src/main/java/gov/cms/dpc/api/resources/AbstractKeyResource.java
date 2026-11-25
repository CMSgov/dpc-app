package gov.cms.dpc.api.resources;

import gov.cms.dpc.api.entities.PublicKeyEntity;
import gov.cms.dpc.api.models.CollectionResponse;
import gov.cms.dpc.api.models.KeySignature;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import java.security.Principal;
import java.util.Optional;
import java.util.UUID;

@Path("/Key")
public abstract class AbstractKeyResource<P extends Principal> {

    @GET
    public abstract CollectionResponse<PublicKeyEntity> getPublicKeys(P organizationPrincipal);

    @GET
    @Path("/{keyID}")
    public abstract PublicKeyEntity getPublicKey(P organizationPrincipal, @NotNull UUID keyID);

    @DELETE
    @Path("/{keyID}")
    public abstract Response deletePublicKey(P organizationPrincipal, @NotNull UUID keyID);

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @POST
    public abstract PublicKeyEntity submitKey(P organizationPrincipal, KeySignature keySignature, Optional<String> keyLabelOptional);
}
