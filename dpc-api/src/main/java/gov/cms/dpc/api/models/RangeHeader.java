package gov.cms.dpc.api.models;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

public class RangeHeader implements Serializable {

    public static final long serialVersionUID = 42L;

    private String unit;
    private Integer start;
    private Integer end;

    public RangeHeader() {
        // Jackson required
    }

    public Integer getStart() {
        return start;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RangeHeader that = (RangeHeader) o;
        return unit.equals(that.unit) &&
                start.equals(that.start) &&
                Objects.equals(end, that.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(unit, start, end);
    }

    @Override
    public String toString() {
        final String endString;
        if (this.end == null) {
            endString = "";
        } else {
            endString = this.end.toString();
        }
        return String.format("%s=%d-%s", this.unit, this.start, endString);
    }
}
