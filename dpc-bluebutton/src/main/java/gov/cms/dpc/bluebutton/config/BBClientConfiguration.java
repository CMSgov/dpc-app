package gov.cms.dpc.bluebutton.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import gov.cms.dpc.fhir.configuration.FHIRClientConfiguration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public class BBClientConfiguration extends FHIRClientConfiguration {

    @NotEmpty
    @JsonProperty("healthcheck")
    private String healthcheckName = "BlueButtonHealthCheck";

    @JsonProperty("registerHealthCheck")
    private boolean registerHealthCheck = false;

    @Min(10)
    @Max(1000)
    private int resourcesCount = 100;

    @Valid
    @NotNull
    @JsonProperty("keyStore")
    private KeystoreConfiguration keystore = new KeystoreConfiguration();

    private boolean useBfdMock = false;

    public int getResourcesCount() { return resourcesCount; }

    public KeystoreConfiguration getKeystore() {
        return keystore;
    }

    public BBClientConfiguration() {
        // Not used
    }

    public String getHealthcheckName() {
        return healthcheckName;
    }

    public void setHealthcheckName(String healthcheckName) {
        this.healthcheckName = healthcheckName;
    }


    public boolean isRegisterHealthCheck() {
        return registerHealthCheck;
    }

    public void setRegisterHealthCheck(boolean registerHealthCheck) {
        this.registerHealthCheck = registerHealthCheck;
    }

    public boolean isUseBfdMock() {
        return useBfdMock;
    }

    public static class KeystoreConfiguration {

        @NotEmpty
        private String type;
        @NotNull
        private String defaultPassword;
        private String location;

        KeystoreConfiguration() {
            // Jackson required
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getDefaultPassword() {
            return defaultPassword;
        }

        public void setDefaultPassword(String defaultPassword) {
            this.defaultPassword = defaultPassword;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }
    }
}
