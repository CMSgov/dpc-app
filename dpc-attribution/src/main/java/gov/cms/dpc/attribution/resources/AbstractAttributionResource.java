package gov.cms.dpc.attribution.resources;

import gov.cms.dpc.common.utils.PropertiesProvider;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

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
    public boolean checkHealth() {
        return true;
    }

    @GET
    @Path("/version")
    public String getVersion() {
        return this.pp.getBuildVersion();
    }
}
