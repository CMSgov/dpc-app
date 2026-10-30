package gov.cms.dpc.attribution.resources.r4;

import gov.cms.dpc.common.annotations.Public;
import gov.cms.dpc.common.utils.PropertiesProvider;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/r4")
@Produces("application/fhir+json")
public class BaseResource {

    private PropertiesProvider pp;

    @Inject
    public BaseResource() {
        this.pp = new PropertiesProvider();
    }

    /**
     * Returns the current build version.
     *
     * @return - {@link String} version number
     */
    @Public
    @GET
    @Path("/version")
    @Consumes(value = "*/*")
    @Produces(MediaType.TEXT_PLAIN)
    public String version() {
        return this.pp.getBuildVersion();
    }
}
