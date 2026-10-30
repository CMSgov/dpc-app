package gov.cms.dpc.api.resources.r4;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.api.auth.annotations.Authorizer;
import io.swagger.annotations.ApiOperation;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

public class GroupResource {

    @POST
    @Timed
    @ExceptionMetered
    @Authorizer
    @ApiOperation(value = "Create Attribution Group", notes = "Not yet implemented.")
    public Response createRoster() {
        return notImplemented();
    }

    @PUT
    @Path("/{rosterID}")
    @Timed
    @ExceptionMetered
    @Authorizer
    @ApiOperation(value = "Replace Attribution Group members", notes = "Not yet implemented.")
    public Response replaceRoster() {
        return notImplemented();
    }

    @DELETE
    @Path("/{rosterID}")
    @Timed
    @ExceptionMetered
    @Authorizer
    @ApiOperation(value = "Delete Attribution Group", notes = "Not yet implemented.")
    public Response deleteRoster() {
        return notImplemented();
    }

    @POST
    @Path("/{rosterID}/$member-add")
    @Timed
    @ExceptionMetered
    @Authorizer
    @ApiOperation(value = "Add a member to an Attribution Group", notes = "Not yet implemented.")
    public Response memberAdd() {
        return notImplemented();
    }

    @POST
    @Path("/{rosterID}/$member-remove")
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
