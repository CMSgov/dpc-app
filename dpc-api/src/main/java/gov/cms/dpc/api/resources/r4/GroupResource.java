package gov.cms.dpc.api.resources.r4;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.api.auth.annotations.Authorizer;
import gov.cms.dpc.fhir.annotations.FHIR;
import io.swagger.annotations.ApiOperation;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

public class GroupResource {

    @POST
    @FHIR
    @Timed
    @ExceptionMetered
    @Authorizer
    @ApiOperation(value = "Create Attribution Group", notes = "Not yet implemented.")
    public Response createRoster() {
        return notImplemented();
    }

    @GET
    @Path("/{rosterID}")
    @FHIR
    @Timed
    @ExceptionMetered
    @Authorizer
    @ApiOperation(value = "Fetch Attribution Group", notes = "Not yet implemented.")
    public Response getRoster() {
        return notImplemented();
    }

    @PUT
    @Path("/{rosterID}")
    @FHIR
    @Timed
    @ExceptionMetered
    @Authorizer
    @ApiOperation(value = "Create or update Attribution Group", notes = "Not yet implemented.")
    public Response updateRoster() {
        return notImplemented();
    }

    @POST
    @Path("/{rosterID}/$member-add")
    @FHIR
    @Timed
    @ExceptionMetered
    @Authorizer
    @ApiOperation(value = "Add a member to an Attribution Group", notes = "Not yet implemented.")
    public Response memberAdd() {
        return notImplemented();
    }

    @POST
    @Path("/{rosterID}/$member-remove")
    @FHIR
    @Timed
    @ExceptionMetered
    @Authorizer
    @ApiOperation(value = "Remove a member from an Attribution Group", notes = "Not yet implemented.")
    public Response memberRemove() {
        return notImplemented();
    }

    private static Response notImplemented() {
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }
}
