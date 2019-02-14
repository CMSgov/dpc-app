package gov.cms.dpc.attribution.resources;

import javax.ws.rs.Path;

public abstract class AbstractAttributionResource {

    AbstractAttributionResource() {
//        Not used
    }

    @Path("/Group")
    public abstract AbstractGroupResource groupOperations();


}
