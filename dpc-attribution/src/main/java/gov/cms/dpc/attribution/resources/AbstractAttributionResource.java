package gov.cms.dpc.attribution.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

public abstract class AbstractAttributionResource {

    protected AbstractAttributionResource() {
//        Not used
    }

    @Path("/Group")
    public abstract AbstractGroupResource groupOperations();

    @Path("/Organization")
    public abstract AbstractOrganizationResource orgOperations();

    @GET
    @Path("/_healthy")
    public boolean checkHealth() {
        return true;
    }
}
