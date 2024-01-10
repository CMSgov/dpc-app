package gov.cms.dpc.api.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;

public class CreateIpAddressRequest {
    private String label;

    @NotNull
    private String ipAddress;

    public String getLabel() {return this.label;}
    public CreateIpAddressRequest setLabel(String label) {this.label = label; return this;}
    public String getIpAddress() {return this.ipAddress;}
    public CreateIpAddressRequest setIpAddress(String ipAddress) {this.ipAddress = ipAddress; return this;}

    public CreateIpAddressRequest(@JsonProperty("ipAddress") String ip_address, @JsonProperty("label") String label) {
        this.setIpAddress(ip_address);
        this.setLabel(label);
    }
    public CreateIpAddressRequest(String ipAddress) {
        this.setIpAddress(ipAddress);
        this.setLabel(null);
    }
}
