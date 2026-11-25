package gov.cms.dpc.api.resources;

import gov.cms.dpc.api.entities.IpAddressEntity;
import gov.cms.dpc.api.models.CollectionResponse;
import gov.cms.dpc.api.models.CreateIpAddressRequest;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import java.security.Principal;
import java.util.UUID;

@Path("/IpAddress")
public abstract class AbstractIpAddressResource<P extends Principal> {
    public abstract CollectionResponse<IpAddressEntity> getOrganizationIpAddresses(P organizationPrincipal);

    public abstract IpAddressEntity submitIpAddress(P organizationPrincipal, CreateIpAddressRequest createIpAddressRequest);

    public abstract Response deleteIpAddress(P organizationPrincipal, @NotNull UUID ipAddressId);
}
