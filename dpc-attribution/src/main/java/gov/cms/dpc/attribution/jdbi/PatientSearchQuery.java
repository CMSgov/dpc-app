package gov.cms.dpc.attribution.jdbi;

import java.util.Optional;
import java.util.UUID;

public class PatientSearchQuery {
    private UUID resourceID;
    private String patientMBI;
    private UUID organizationID;
    private Integer pageOffset;
    private Integer count;

    public PatientSearchQuery() {
        // constructor left empty in order to make instantiation simpler for optional params
    }

    public void setResourceID(UUID resourceID) {
        this.resourceID = resourceID;
    }

    public Optional<UUID> getResourceID() {
        return Optional.ofNullable(this.resourceID);
    }

    public void setPatientMBI(String patientMBI) {
        this.patientMBI = patientMBI;
    }

    public Optional<String> getPatientMBI() {
        return Optional.ofNullable(this.patientMBI);
    }

    public void setOrganizationID(UUID organizationID) {
        this.organizationID = organizationID;
    }

    public Optional<UUID> getOrganizationID() {
        return Optional.ofNullable(this.organizationID);
    }

    public void setPageOffset(int pageOffset) { this.pageOffset = pageOffset; }

    public Optional<Integer> getPageOffset() {
        return Optional.ofNullable(this.pageOffset);
    }

    public void setCount(int count) {
        this.count = count;
    }

    public Optional<Integer> getCount() {
        return Optional.ofNullable(this.count);
    }
}
