package gov.cms.dpc.api.resources;

import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.entities.IpAddressEntity;
import gov.cms.dpc.api.models.CollectionResponse;
import gov.cms.dpc.api.models.CreateIpAddressRequest;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

@Path("/IpAddress")
public abstract class AbstractIpAddressResource {
    public abstract CollectionResponse<IpAddressEntity> getOrganizationIpAddresses(OrganizationPrincipal organizationPrincipal);

    public abstract IpAddressEntity submitIpAddress(OrganizationPrincipal organizationPrincipal, CreateIpAddressRequest createIpAddressRequest);

    public abstract Response deleteIpAddress(OrganizationPrincipal organizationPrincipal, @NotNull UUID ipAddressId);
}
