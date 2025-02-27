package gov.cms.dpc.common.entities;

import gov.cms.dpc.common.annotations.NoHtml;
import org.hl7.fhir.dstu3.model.Address;
import org.hl7.fhir.dstu3.model.StringType;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Embeddable
public class AddressEntity implements Serializable {

    public static final long serialVersionUID = 42L;

    @NotNull
    @Column(name = "address_use")
    private Address.AddressUse use;
    @NotNull
    @Column(name = "address_type")
    private Address.AddressType type;
    @NoHtml
    @NotEmpty
    private String line1;
    @NoHtml
    private String line2;
    @NoHtml
    private String city;
    @NoHtml
    private String district;
    @NoHtml
    private String state;
    @NoHtml
    @Column(name = "postal_code")
    private String postalCode;
    @NoHtml
    private String country;

    public AddressEntity() {
        // Not used
    }

    public Address.AddressUse getUse() {
        return use;
    }

    public void setUse(Address.AddressUse use) {
        this.use = use;
    }

    public Address.AddressType getType() {
        return type;
    }

    public void setType(Address.AddressType type) {
        this.type = type;
    }

    public String getLine1() {
        return line1;
    }

    public void setLine1(String line1) {
        this.line1 = line1;
    }

    public String getLine2() {
        return line2;
    }

    public void setLine2(String line2) {
        this.line2 = line2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Address toFHIR() {
        final Address address = new Address();

        address.setCity(this.city);
        address.setCountry(this.country);
        address.setDistrict(this.district);
        address.setPostalCode(this.postalCode);
        address.setState(this.state);
        address.setUse(this.use);
        address.setType(this.type);
        address.setLine(List.of(new StringType(this.line1), new StringType(this.line2)));

        return address;
    }
}
