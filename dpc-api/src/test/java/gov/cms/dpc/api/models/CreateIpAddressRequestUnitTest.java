package gov.cms.dpc.api.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CreateIpAddressRequestUnitTest {
    @Test
    public void testConstructor() {
        CreateIpAddressRequest createIpAddressRequest = new CreateIpAddressRequest("192.168.1.1");
        assertEquals("192.168.1.1", createIpAddressRequest.getIpAddress());
    }

    @Test
    public void testSettersAndSetters() {
        CreateIpAddressRequest createIpAddressRequest = new CreateIpAddressRequest("192.168.1.1");

        createIpAddressRequest.setIpAddress("10.1.1.1");
        assertEquals("10.1.1.1", createIpAddressRequest.getIpAddress());
    }
}
