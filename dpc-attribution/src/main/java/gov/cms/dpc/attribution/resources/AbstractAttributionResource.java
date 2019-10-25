package gov.cms.dpc.attribution.resources;

import gov.cms.dpc.common.utils.PropertiesProvider;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Api(value = "Metadata")
public abstract class AbstractAttributionResource {

    private final PropertiesProvider pp;

    protected AbstractAttributionResource() {
        this.pp = new PropertiesProvider();
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

    @GET
    @Path("/_healthy")
    @ApiOperation(value = "Check is healthy", notes = "Returns whether or not the application is in a healthy state." +
            "\n\nMeaning, are all endpoints functioning and is the attribution database reachable.")
    public boolean checkHealth() {
        return true;
    }

    @GET
    @Path("/version")
    @ApiOperation(value = "Get application build version", notes = "Returns the application build version. " +
            "Which is the git sha abbreviation and the build timestamp.")
    public String getVersion() {
        return this.pp.getBuildVersion();
    }
}
