package gov.cms.dpc.common.entities;

import org.hibernate.validator.constraints.NotEmpty;
import org.hl7.fhir.dstu3.model.Address;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Embeddable
public class AddressEntity implements Serializable {

    public static final long serialVersionUID = 42L;

    @NotNull
    @Column(name = "address_use")
    private Address.AddressUse use;
    @NotNull
    @Column(name = "address_type")
    private Address.AddressType type;
    @NotEmpty
    private String line1;
    private String line2;
    private String city;
    private String district;
    private String state;
    @Column(name = "postal_code")
    private String postalCode;
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
}
