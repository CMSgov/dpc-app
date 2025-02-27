package gov.cms.dpc.api.resources;

import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.common.annotations.NoHtml;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Produces("application/json")
@Path("/Jobs")
public abstract class AbstractJobResource {

    protected AbstractJobResource() {
        // Not used
    }

    @Path("/{jobID}")
    @GET
    public abstract Response checkJobStatus(OrganizationPrincipal organizationPrincipal, @NoHtml String jobID);
}
