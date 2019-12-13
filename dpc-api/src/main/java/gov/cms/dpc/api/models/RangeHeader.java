package gov.cms.dpc.api.models;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

public class RangeHeader implements Serializable {

    public static final long serialVersionUID = 42L;

    private String unit;
    private Long start;
    private Long end;

    public RangeHeader() {
        // Jackson required
    }

    public Long getStart() {
        return start;
    }

    public void setStart(Long start) {
        this.start = start;
    }

    public Optional<Long> getEnd() {
        return Optional.ofNullable(this.end);
    }

    public void setEnd(Long end) {
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
        if (!(o instanceof RangeHeader)) return false;
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
