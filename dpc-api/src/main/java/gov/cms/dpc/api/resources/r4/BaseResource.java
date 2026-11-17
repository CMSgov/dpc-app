package gov.cms.dpc.api.resources.r4;

import jakarta.inject.Inject;
import jakarta.ws.rs.Path;

@Path("/v3")
public class BaseResource {

    private final GroupResource groupResource;
    private final DataResource dataResource;

    @Inject
    public BaseResource(GroupResource groupResource, DataResource dataResource) {
        this.groupResource = groupResource;
        this.dataResource = dataResource;
    }

    @Path("/Group")
    public GroupResource groupOperations() {
        return groupResource;
    }

    @Path("/Data")
    public DataResource dataOperations() {
        return dataResource;
    }
}
