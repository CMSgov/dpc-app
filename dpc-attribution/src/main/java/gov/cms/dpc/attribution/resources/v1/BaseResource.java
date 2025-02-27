package gov.cms.dpc.attribution.resources.v1;

import gov.cms.dpc.attribution.resources.AbstractBaseResource;
import gov.cms.dpc.common.annotations.Public;
import gov.cms.dpc.common.utils.PropertiesProvider;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;


@Path("/v1")
public class BaseResource extends AbstractBaseResource {

    private PropertiesProvider pp;

    @Inject
    public BaseResource() {
        this.pp = new PropertiesProvider();
    }

    @Override
    @Public
    @GET
    @Path("/version")
    @Consumes(value = "*/*")
    @Produces(MediaType.TEXT_PLAIN)
    public String version() {
        return this.pp.getBuildVersion();
    }
}
