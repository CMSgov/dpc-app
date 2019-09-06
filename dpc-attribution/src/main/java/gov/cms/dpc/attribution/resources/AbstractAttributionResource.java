package gov.cms.dpc.attribution.resources;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Api(value = "Health")
public abstract class AbstractAttributionResource {

    protected AbstractAttributionResource() {
//        Not used
    }

    @Path("/Group")
    public abstract AbstractGroupResource groupOperations();

    @Path("/Organization")
    public abstract AbstractOrganizationResource orgOperations();

    @Path("/Endpoint")
    public abstract AbstractEndpointResource endpointOperations();

    @Path("/Patient")
    public abstract AbstractPatientResource patientOperations();

    @Path("/Practitioner")
    public abstract AbstractPractitionerResource providerOperations();

    @Path("/Token")
    public abstract AbstractTokenResource tokenOperations();

    @GET
    @Path("/_healthy")
    @ApiOperation(value = "Check is healthy", notes = "Returns whether or not the application is in a healthy state." +
            "\n\nMeaning, are all endpoints functioning and is the attribution database reachable.")
    public boolean checkHealth() {
        return true;
    }
}
