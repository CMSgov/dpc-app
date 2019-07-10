package gov.cms.dpc.attribution.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.temporal.ChronoUnit;

public class TokenPolicy {

    @NotNull
    @Valid
    @JsonProperty("versionPolicy")
    private VersionPolicy versionPolicy = new VersionPolicy();

    @NotNull
    @Valid
    @JsonProperty("expirationPolicy")
    private ExpirationPolicy expirationPolicy = new ExpirationPolicy();

    public TokenPolicy() {
        // JacksonRequired
    }

    public VersionPolicy getVersionPolicy() {
        return versionPolicy;
    }

    public void setVersionPolicy(VersionPolicy versionPolicy) {
        this.versionPolicy = versionPolicy;
    }

    public ExpirationPolicy getExpirationPolicy() {
        return expirationPolicy;
    }

    public void setExpirationPolicy(ExpirationPolicy expirationPolicy) {
        this.expirationPolicy = expirationPolicy;
    }

    public static class VersionPolicy {

        @NotNull
        private Integer minimumVersion;
        @NotNull
        private Integer currentVersion;

        public VersionPolicy() {
            // Jackson required
        }

        public Integer getMinimumVersion() {
            return minimumVersion;
        }

        public void setMinimumVersion(Integer minimumVersion) {
            this.minimumVersion = minimumVersion;
        }

        public Integer getCurrentVersion() {
            return currentVersion;
        }

        public void setCurrentVersion(Integer currentVersion) {
            this.currentVersion = currentVersion;
        }
    }

    public static class ExpirationPolicy {

        public ExpirationPolicy() {
            // Jackson required
        }

        @NotNull
        private Integer expirationOffset;
        @NotNull
        private ChronoUnit expirationUnit;

        public Integer getExpirationOffset() {
            return expirationOffset;
        }

        public void setExpirationOffset(Integer expirationOffset) {
            this.expirationOffset = expirationOffset;
        }

        public ChronoUnit getExpirationUnit() {
            return expirationUnit;
        }

        public void setExpirationUnit(ChronoUnit expirationUnit) {
            this.expirationUnit = expirationUnit;
        }

        public void setExpirationUnit(String expirationUnit) {
            this.expirationUnit = ChronoUnit.valueOf(expirationUnit);
        }
    }
}
