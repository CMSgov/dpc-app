package gov.cms.dpc.api.resources.r4;

import jakarta.inject.Inject;
import jakarta.ws.rs.Path;

@Path("/v3")
public class BaseResource {

    private final GroupResource groupResource;
    private final DataResource dataResource;
    private final OrganizationResource organizationResource;

    @Inject
    public BaseResource(GroupResource groupResource, DataResource dataResource, OrganizationResource organizationResource) {
        this.groupResource = groupResource;
        this.dataResource = dataResource;
        this.organizationResource = organizationResource;
    }

    @Path("/Group")
    public GroupResource groupOperations() {
        return groupResource;
    }

    @Path("/Data")
    public DataResource dataOperations() {
        return dataResource;
    }

    @Path("/Organization")
    public OrganizationResource organizationOperations() {
        return organizationResource;
    }
}
