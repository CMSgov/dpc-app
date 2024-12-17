package gov.cms.dpc.attribution.resources.v1;

import com.google.inject.Inject;
import gov.cms.dpc.attribution.resources.AbstractBaseResource;
import gov.cms.dpc.common.annotations.Public;
import gov.cms.dpc.common.utils.PropertiesProvider;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;


@Path("/v1")
public class BaseResource extends AbstractBaseResource {

    private final PropertiesProvider pp;

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
