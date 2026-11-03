package gov.cms.dpc.api.resources.r4;

import jakarta.inject.Inject;
import jakarta.ws.rs.Path;

@Path("/v3")
public class BaseResource {

    private final GroupResource groupResource;

    @Inject
    public BaseResource(GroupResource groupResource) {
        this.groupResource = groupResource;
    }

    @Path("/Group")
    public GroupResource groupOperations() {
        return groupResource;
    }
}
