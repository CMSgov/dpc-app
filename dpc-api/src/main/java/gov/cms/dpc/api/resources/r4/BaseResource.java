package gov.cms.dpc.api.resources.r4;

import jakarta.inject.Inject;
import jakarta.ws.rs.Path;

@Path("/v3")
public class BaseResource {

    private final GroupResource groupResource;
    private final DataResource dataResource;
    private final OrganizationResource organizationResource;
    private final PractitionerResource practitionerResource;
    private final PatientResource patientResource;

    @Inject
    public BaseResource(GroupResource groupResource, DataResource dataResource, OrganizationResource organizationResource,
                        PractitionerResource practitionerResource, PatientResource patientResource) {
        this.groupResource = groupResource;
        this.dataResource = dataResource;
        this.organizationResource = organizationResource;
        this.practitionerResource = practitionerResource;
        this.patientResource = patientResource;
    }

    @Path("/Group")
    public GroupResource groupOperations() {
        return groupResource;
    }

    @Path("/Data")
    public DataResource dataOperations() {
        return dataResource;
    }

    @Path("/Organization")
    public OrganizationResource organizationOperations() {
        return organizationResource;
    }

    @Path("/Practitioner")
    public PractitionerResource practitionerOperations() {
        return practitionerResource;
    }

    @Path("/Patient")
    public PatientResource patientOperations() {
        return patientResource;
    }
}
