package gov.cms.dpc.api.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;

public class CreateIpAddressRequest {

    @NotEmpty
    private String ipAddress;

    @JsonProperty("ip_address")
    public String getIpAddress() {return this.ipAddress;}
    @JsonProperty("ip_address")
    public CreateIpAddressRequest setIpAddress(String ipAddress) {this.ipAddress = ipAddress; return this;}

    public CreateIpAddressRequest(@JsonProperty("ip_address") String ipAddress) {
        this.setIpAddress(ipAddress);
    }

}
