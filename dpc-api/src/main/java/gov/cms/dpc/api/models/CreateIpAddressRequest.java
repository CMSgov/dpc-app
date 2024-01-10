package gov.cms.dpc.api.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gov.cms.dpc.api.converters.InetDeserializer;
import io.hypersistence.utils.hibernate.type.basic.Inet;

public class CreateIpAddressRequest {
    private String label;
    @JsonDeserialize(using = InetDeserializer.class)
    private Inet ipAddress;

    public String getLabel() {return this.label;}
    public CreateIpAddressRequest setLabel(String label) {this.label = label; return this;}
    public Inet getIpAddress() {return this.ipAddress;}
    public CreateIpAddressRequest setIpAddress(Inet ipAddress) {this.ipAddress = ipAddress; return this;}

    public CreateIpAddressRequest(@JsonProperty("ipAddress") Inet ipAddress, @JsonProperty("label") String label) {
        this.setIpAddress(ipAddress);
        this.setLabel(label);
    }
    public CreateIpAddressRequest(Inet ipAddress) {
        this.setIpAddress(ipAddress);
        this.setLabel(null);
    }
}
