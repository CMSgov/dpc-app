package gov.cms.dpc.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Produces("application/fhir+json")
public abstract class AbstractBaseResource {

    public AbstractBaseResource() {
//        Not used
    }

    /**
     * Returns the current API version
     *
     * @return - {@link String} version number
     */
    @Path("/version")
    @Produces(MediaType.TEXT_PLAIN)
    @GET
    public abstract String version();

    /**
     * Returns the FHIR capabilities statement
     *
     * @return {@link String} capabilities statement
     */
    @Path("/metadata")
    @GET
    public abstract String metadata();

    @Path("/Group")
    public abstract AbstractGroupResource groupOperations();
}
