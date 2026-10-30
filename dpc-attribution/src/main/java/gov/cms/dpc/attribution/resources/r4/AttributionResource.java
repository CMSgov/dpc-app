package gov.cms.dpc.attribution.resources.r4;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/r4")
public class AttributionResource {

    private final OrganizationResource or;
    private final PractitionerResource pr;
    private final PatientResource patr;
    private final GroupResource gr;

    @Inject
    public AttributionResource(OrganizationResource or,
                               PractitionerResource pr,
                               PatientResource patr,
                               GroupResource gr) {
        this.or = or;
        this.pr = pr;
        this.patr = patr;
        this.gr = gr;
    }

    @Path("/Organization")
    public OrganizationResource orgOperations() {
        return this.or;
    }

    @Path("/Practitioner")
    public PractitionerResource providerOperations() {
        return this.pr;
    }

    @Path("/Patient")
    public PatientResource patientOperations() {
        return this.patr;
    }

    @Path("/Group")
    public GroupResource groupOperations() {
        return this.gr;
    }

    @GET
    @Path("/_healthy")
    public boolean checkHealth() {
        return true;
    }
}
