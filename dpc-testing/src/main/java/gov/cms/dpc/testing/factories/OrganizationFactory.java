package gov.cms.dpc.testing.factories;

import org.hl7.fhir.dstu3.model.Address;
import org.hl7.fhir.dstu3.model.Organization;

/**
 * A small collection of helper classes for creating fake (but valid) {@link Organization} resources.
 * It's completely deterministic, but at least it makes the FHIR parser happy
 */
public class OrganizationFactory {

    private OrganizationFactory() {
        // Not used
    }

    public static Address generateFakeAddress() {
        final Address address = new Address();
        address.addLine("1800 Pennsylvania Ave NW");
        address.setCity("Washington");
        address.setState("DC");
        address.setPostalCode("20006");
        address.setCountry("US");
        address.setUse(Address.AddressUse.WORK);
        address.setType(Address.AddressType.PHYSICAL);

        return address;
    }

    public static Organization generateFakeOrganization() {
        final Organization organization = new Organization();
        organization.setId("test-organization");
        organization.setName("Test Organization");

        return organization;
    }
}
