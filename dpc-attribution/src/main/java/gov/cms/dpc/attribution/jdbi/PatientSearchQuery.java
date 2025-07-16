package gov.cms.dpc.attribution.jdbi;

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

    public UUID getResourceID() {
        return this.resourceID;
    }

    public void setPatientMBI(String patientMBI) {
        this.patientMBI = patientMBI;
    }

    public String getPatientMBI() {
        return this.patientMBI;
    }

    public void setOrganizationID(UUID organizationID) {
        this.organizationID = organizationID;
    }

    public UUID getOrganizationID() {
        return this.organizationID;
    }

    public void setPageOffset(int pageOffset) { this.pageOffset = pageOffset; }

    public Integer getPageOffset() {
        return this.pageOffset;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public Integer getCount() {
        return this.count;
    }
}
