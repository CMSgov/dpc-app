package gov.cms.dpc.core;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

public abstract class AbstractApplicationRoutes {

    public AbstractApplicationRoutes() {
//        Not used
    }

    /**
     * Returns the current API version
     * @return - {@link String} version number
     */
    @Path("/version")
    @GET
    public abstract String version();

    /**
     * Returns the FHIR capabilities statement
     * @return {@link String} capabilities statement
     */
    @Path("/metadata")
    @GET
    @Produces("application/json")
    public abstract String metadata();
}
