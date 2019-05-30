package gov.cms.dpc.attribution.models;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class AttributionCheckRequest implements Serializable {

    public static final long serialVersionUID = 42L;

    private String groupID;
    private List<String> patientIDs;

    public AttributionCheckRequest() {
        // Not used
    }

    public String getGroupID() {
        return groupID;
    }

    public void setGroupID(String groupID) {
        this.groupID = groupID;
    }

    public List<String> getPatientIDs() {
        return patientIDs;
    }

    public void setPatientIDs(List<String> patientIDs) {
        this.patientIDs = patientIDs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AttributionCheckRequest that = (AttributionCheckRequest) o;
        return Objects.equals(groupID, that.groupID) &&
                Objects.equals(patientIDs, that.patientIDs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupID, patientIDs);
    }
}
