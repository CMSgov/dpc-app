package gov.cms.dpc.bluebutton.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class BBClientConfiguration {

    @NotEmpty
    @JsonProperty("healthcheck")
    private String healthcheckName = "BlueButtonHealthCheck";

    @JsonProperty("registerHealthCheck")
    private boolean registerHealthCheck = false;

    @NotEmpty
    private String serverBaseUrl;

    @Valid
    @NotNull
    @JsonProperty("timeouts")
    private TimeoutConfiguration timeouts = new TimeoutConfiguration();

    @Min(10)
    @Max(1000)
    private int resourcesCount = 100;

    @Valid
    @NotNull
    @JsonProperty("keyStore")
    private KeystoreConfiguration keystore = new KeystoreConfiguration();

    @NotEmpty
    private String bfdHashPepper;

    private int bfdHashIter;

    private boolean useBfdMock = false;

    private R4Configuration r4Configuration;

    public R4Configuration getR4Configuration() {
        return r4Configuration;
    }

    public TimeoutConfiguration getTimeouts() {
        return timeouts;
    }

    public int getResourcesCount() { return resourcesCount; }

    public KeystoreConfiguration getKeystore() {
        return keystore;
    }

    public BBClientConfiguration() {
        // Not used
    }

    public String getServerBaseUrl() {
        return serverBaseUrl;
    }

    public void setServerBaseUrl(String serverBaseUrl) {
        this.serverBaseUrl = serverBaseUrl;
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

    public String getBfdHashPepper() { return bfdHashPepper; }

    public int getBfdHashIter() { return bfdHashIter; }

    public boolean isUseBfdMock() {
        return useBfdMock;
    }

    public static class TimeoutConfiguration {

        private Integer connectionTimeout;
        private Integer socketTimeout;
        private Integer requestTimeout;

        TimeoutConfiguration() {
            // Jackson required
        }

        public Integer getConnectionTimeout() {
            return connectionTimeout;
        }

        public void setConnectionTimeout(Integer connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
        }

        public Integer getSocketTimeout() {
            return socketTimeout;
        }

        public void setSocketTimeout(Integer socketTimeout) {
            this.socketTimeout = socketTimeout;
        }

        public Integer getRequestTimeout() {
            return requestTimeout;
        }

        public void setRequestTimeout(Integer requestTimeout) {
            this.requestTimeout = requestTimeout;
        }
    }

    public static class R4Configuration {
        private String serverBaseUrl;

        public R4Configuration() {
            // Not used
        }

        public String getServerBaseUrl() {
            return serverBaseUrl;
        }
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
