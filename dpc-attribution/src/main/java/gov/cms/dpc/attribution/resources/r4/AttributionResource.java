package gov.cms.dpc.attribution.resources.r4;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/r4")
public class AttributionResource {

    private final OrganizationResource or;
    private final PractitionerResource pr;

    @Inject
    public AttributionResource(OrganizationResource or,
                               PractitionerResource pr) {
        this.or = or;
        this.pr = pr;
    }

    @Path("/Organization")
    public OrganizationResource orgOperations() {
        return this.or;
    }

    @Path("/Practitioner")
    public PractitionerResource providerOperations() {
        return this.pr;
    }

    @GET
    @Path("/_healthy")
    public boolean checkHealth() {
        return true;
    }
}
