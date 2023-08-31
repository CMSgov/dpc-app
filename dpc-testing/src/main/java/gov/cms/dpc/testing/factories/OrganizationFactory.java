package gov.cms.dpc.testing.factories;

import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.dstu3.model.codesystems.EndpointConnectionType;

import java.util.List;

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
        address.setUse(Address.AddressUse.HOME);
        address.setType(Address.AddressType.PHYSICAL);

        return address;
    }

    public static Organization generateFakeOrganization() {
        final Organization organization = new Organization();
        organization.addEndpoint(new Reference("Endpoint/test-endpoint"));

        organization.setId("test-organization");
        organization.setName("Test Organization");

        return organization;
    }

    public static Endpoint createFakeEndpoint() {
        final Endpoint endpoint = new Endpoint();

        // Payload type concept
        final CodeableConcept payloadType = new CodeableConcept();
        payloadType.addCoding().setCode("nothing").setSystem("http://nothing.com");

        endpoint.setPayloadType(List.of(payloadType));

        endpoint.setId("test-endpoint");
        endpoint.setConnectionType(new Coding(EndpointConnectionType.HL7FHIRREST.getSystem(), EndpointConnectionType.HL7FHIRREST.toCode(), EndpointConnectionType.HL7FHIRREST.getDisplay()));
        endpoint.setStatus(Endpoint.EndpointStatus.ACTIVE);
        return endpoint;
    }

    public static Endpoint createValidFakeEndpoint() {
        Endpoint endpoint = createFakeEndpoint();
        endpoint.setId((String)null);
        endpoint.setName("Fake Endpoint");
        endpoint.setManagingOrganization(new Reference(new IdType("Organization", "46ac7ad6-7487-4dd0-baa0-6e2c8cae76a0")));
        endpoint.setAddress("http://www.example.com/endpoint");
        return endpoint;
    }


    public static Endpoint createValidFakeEndpoint(String organizationId) {
        Endpoint endpoint = createValidFakeEndpoint();
        endpoint.setManagingOrganization(new Reference(new IdType("Organization", organizationId)));
        return endpoint;
    }
}
