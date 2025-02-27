package gov.cms.dpc.api.resources;

import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.common.annotations.NoHtml;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

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
