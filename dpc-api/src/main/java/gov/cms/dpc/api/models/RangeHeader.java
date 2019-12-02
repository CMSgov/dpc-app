package gov.cms.dpc.api.models;

import java.io.Serializable;
import java.util.Optional;

public class RangeHeader implements Serializable {

    public static final long serialVersionUID = 42L;

    private String unit;
    private Integer start;
    private Integer end;

    public RangeHeader() {
        // Jackson required
    }

    public Optional<Integer> getStart() {
        return Optional.ofNullable(start);
    }

    public void setStart(Integer start) {
        this.start = start;
    }

    public Optional<Integer> getEnd() {
        return Optional.ofNullable(this.end);
    }

    public void setEnd(Integer end) {
        this.end = end;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }
}
