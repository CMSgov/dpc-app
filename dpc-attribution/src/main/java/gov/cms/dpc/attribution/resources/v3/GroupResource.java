package gov.cms.dpc.attribution.resources.v3;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

public class GroupResource {

    @POST
    public Response createRoster() {
        return notImplemented();
    }

    @PUT
    @Path("/{rosterID}")
    public Response replaceRoster() {
        return notImplemented();
    }

    @DELETE
    @Path("/{rosterID}")
    public Response deleteRoster() {
        return notImplemented();
    }

    @POST
    @Path("/{rosterID}/$member-add")
    public Response memberAdd() {
        return notImplemented();
    }

    @POST
    @Path("/{rosterID}/$member-remove")
    public Response memberRemove() {
        return notImplemented();
    }

    private static Response notImplemented() {
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }
}
