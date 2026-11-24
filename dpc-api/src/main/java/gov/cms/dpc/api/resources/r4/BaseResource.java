package gov.cms.dpc.api.resources.r4;

import jakarta.inject.Inject;
import jakarta.ws.rs.Path;

@Path("/v3")
public class BaseResource {

    private final GroupResource groupResource;
    private final TokenResource tokenResource;
    private final KeyResource keyResource;
    private final IpAddressResource ipAddressResource;
    private final AdminResource adminResource;

    @Inject
    public BaseResource(GroupResource groupResource, TokenResource tokenResource,
                        KeyResource keyResource, IpAddressResource ipAddressResource,
                        AdminResource adminResource) {
        this.groupResource = groupResource;
        this.tokenResource = tokenResource;
        this.keyResource = keyResource;
        this.ipAddressResource = ipAddressResource;
        this.adminResource = adminResource;
    }

    @Path("/Group")
    public GroupResource groupOperations() {
        return groupResource;
    }

    @Path("/Token")
    public TokenResource tokenOperations() {
        return tokenResource;
    }

    @Path("/Key")
    public KeyResource keyOperations() {
        return keyResource;
    }

    @Path("/IpAddress")
    public IpAddressResource ipAddressOperations() {
        return ipAddressResource;
    }

    @Path("/Admin")
    public AdminResource adminOperations() {
        return adminResource;
    }
}
