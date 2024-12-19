package gov.cms.dpc.consent.resources;

import gov.cms.dpc.common.annotations.Public;
import gov.cms.dpc.common.utils.PropertiesProvider;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;


@Api(value = "Metadata")
@Path("/v1")
public class BaseResource {

    private final PropertiesProvider pp;

    @Inject
    public BaseResource() {
        this.pp = new PropertiesProvider();
    }

    @Public
    @GET
    @Path("/version")
    @ApiOperation(value = "Return the application build version")
    @Consumes(value = "*/*")
    @Produces(MediaType.TEXT_PLAIN)
    public String version() {
        return this.pp.getBuildVersion();
    }
}
