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
    private final TokenResource tokenResource;
    private final KeyResource keyResource;
    private final IpAddressResource ipAddressResource;

    @Inject
    public BaseResource(
        GroupResource groupResource,
        DataResource dataResource,
        OrganizationResource organizationResource,
        PractitionerResource practitionerResource,
        PatientResource patientResource,
        TokenResource tokenResource,
        KeyResource keyResource,
        IpAddressResource ipAddressResource
    ) {
        this.groupResource = groupResource;
        this.dataResource = dataResource;
        this.organizationResource = organizationResource;
        this.practitionerResource = practitionerResource;
        this.patientResource = patientResource;
        this.tokenResource = tokenResource;
        this.keyResource = keyResource;
        this.ipAddressResource = ipAddressResource;
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

    @Path("/Token")
    public TokenResource tokenOperations() {
        return tokenResource;
    }

    @Path("/Key")
    public KeyResource keyOperations() {
        return keyResource;
    }

    @Path("/IpAddress")
    public IpAddressResource ipAddressOperations() {
        return ipAddressResource;
    }
}
