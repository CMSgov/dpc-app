package gov.cms.dpc.api.entities;

import io.hypersistence.utils.hibernate.type.basic.Inet;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IpAddressEntityTest {

    @Test
    void testGettersAndSetters() {
        IpAddressEntity ipAddress = new IpAddressEntity();
        UUID id = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        String ip = "192.0.0.1";

        ipAddress.setId(id);
        ipAddress.setOrganizationId(orgId);
        ipAddress.setIpAddress(new Inet(ip));

        assertEquals(id, ipAddress.getId());
        assertEquals(orgId, ipAddress.getOrganizationId());
        assertEquals(ip, ipAddress.getIpAddress().getAddress());
    }
}
