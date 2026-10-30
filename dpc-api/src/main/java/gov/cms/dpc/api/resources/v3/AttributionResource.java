package gov.cms.dpc.api.resources.v3;

import jakarta.inject.Inject;
import jakarta.ws.rs.Path;

@Path("/v3")
public class AttributionResource {

    private final GroupResource groupResource;

    @Inject
    public AttributionResource(GroupResource groupResource) {
        this.groupResource = groupResource;
    }

    @Path("/Group")
    public GroupResource groupOperations() {
        return groupResource;
    }
}
