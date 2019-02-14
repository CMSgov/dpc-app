package gov.cms.dpc.attribution.resources.v1;

import gov.cms.dpc.attribution.engine.AttributionEngine;
import gov.cms.dpc.attribution.resources.AbstractGroupResource;
import org.eclipse.jetty.http.HttpStatus;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.Set;

public class GroupResource extends AbstractGroupResource {

    private final AttributionEngine engine;

    @Inject
    GroupResource(AttributionEngine engine) {
        this.engine = engine;
    }

    @Path("/{groupID}")
    @GET
    @Override
    public Response getAttributedPatients(@PathParam("groupID") String groupID, @Context HttpServletRequest req) {
        final Set<String> beneficiaries = engine.getAttributedBeneficiaries(groupID);
        return Response.status(HttpStatus.OK_200).entity(beneficiaries).build();
    }

    @Path("/test")
    @GET
    public String getTest() {
        return "hello world";
    }
}
