package gov.cms.dpc.api.resources.r4;

import jakarta.inject.Inject;
import jakarta.ws.rs.Path;

@Path("/v3")
public class BaseResource {

    private final GroupResource groupResource;
    private final TokenResource tokenResource;
    private final KeyResource keyResource;

    @Inject
    public BaseResource(GroupResource groupResource, TokenResource tokenResource, KeyResource keyResource) {
        this.groupResource = groupResource;
        this.tokenResource = tokenResource;
        this.keyResource = keyResource;
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
}
