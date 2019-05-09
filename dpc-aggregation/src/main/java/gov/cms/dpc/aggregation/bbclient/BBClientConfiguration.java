package gov.cms.dpc.aggregation.bbclient;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class BBClientConfiguration {

    @NotEmpty
    private String serverBaseUrl;

    @Valid
    @NotNull
    @JsonProperty("timeouts")
    private TimeoutConfiguration timeouts = new TimeoutConfiguration();

    @Valid
    @NotNull
    @JsonProperty("keyStore")
    private KeystoreConfiguration keystore = new KeystoreConfiguration();

    public TimeoutConfiguration getTimeouts() {
        return timeouts;
    }

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
