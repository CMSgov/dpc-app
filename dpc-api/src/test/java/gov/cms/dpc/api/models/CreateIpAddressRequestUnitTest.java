package gov.cms.dpc.api.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Inet access")
class CreateIpAddressRequestUnitTest {
    @Test
    @DisplayName("Construct IP address request 🥳")
    public void testConstructor() {
        CreateIpAddressRequest createIpAddressRequest = new CreateIpAddressRequest("192.168.1.1", "label");
        assertEquals("192.168.1.1", createIpAddressRequest.getIpAddress());
        assertEquals("label", createIpAddressRequest.getLabel());
    }

    @Test
    @DisplayName("Construct IP address request without label 🥳")
    public void testAltConstructor() {
        CreateIpAddressRequest createIpAddressRequest = new CreateIpAddressRequest("192.168.1.1");
        assertEquals("192.168.1.1", createIpAddressRequest.getIpAddress());
        assertNull(createIpAddressRequest.getLabel());
    }

    @Test
    @DisplayName("Set and get IP address 🥳")
    public void testSettersAndGetters() {
        CreateIpAddressRequest createIpAddressRequest = new CreateIpAddressRequest("192.168.1.1");

        createIpAddressRequest.setIpAddress("10.1.1.1");
        createIpAddressRequest.setLabel("new label");
        assertEquals("10.1.1.1", createIpAddressRequest.getIpAddress());
        assertEquals("new label", createIpAddressRequest.getLabel());
    }
}