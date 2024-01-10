package gov.cms.dpc.api.models;

import io.hypersistence.utils.hibernate.type.basic.Inet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CreateIpAddressRequestUnitTest {
    @Test
    public void testConstructor() {
        CreateIpAddressRequest createIpAddressRequest = new CreateIpAddressRequest(new Inet("192.168.1.1"), "label");
        assertEquals("192.168.1.1", createIpAddressRequest.getIpAddress().getAddress());
        assertEquals("label", createIpAddressRequest.getLabel());
    }

    @Test
    public void testAltConstructor() {
        CreateIpAddressRequest createIpAddressRequest = new CreateIpAddressRequest(new Inet("192.168.1.1"));
        assertEquals("192.168.1.1", createIpAddressRequest.getIpAddress().getAddress());
        assertNull(createIpAddressRequest.getLabel());
    }

    @Test
    public void testSettersAndSetters() {
        CreateIpAddressRequest createIpAddressRequest = new CreateIpAddressRequest(new Inet("192.168.1.1"));

        createIpAddressRequest.setIpAddress(new Inet("10.1.1.1"));
        createIpAddressRequest.setLabel("new label");
        assertEquals("10.1.1.1", createIpAddressRequest.getIpAddress().getAddress());
        assertEquals("new label", createIpAddressRequest.getLabel());
    }
}