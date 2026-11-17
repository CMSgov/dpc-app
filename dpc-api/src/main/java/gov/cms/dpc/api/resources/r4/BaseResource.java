package gov.cms.dpc.api.resources.r4;

import jakarta.inject.Inject;
import jakarta.ws.rs.Path;

@Path("/v3")
public class BaseResource {

    private final GroupResource groupResource;
    private final TokenResource tokenResource;

    @Inject
    public BaseResource(GroupResource groupResource, TokenResource tokenResource) {
        this.groupResource = groupResource;
        this.tokenResource = tokenResource;
    }

    @Path("/Group")
    public GroupResource groupOperations() {
        return groupResource;
    }

    @Path("/Token")
    public TokenResource tokenOperations() {
        return tokenResource;
    }
}
