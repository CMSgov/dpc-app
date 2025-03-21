package gov.cms.dpc.api.jdbi;

import gov.cms.dpc.api.entities.IpAddressEntity;
import gov.cms.dpc.common.hibernate.auth.DPCAuthManagedSessionFactory;
import gov.cms.dpc.testing.AbstractDAOTest;
import io.hypersistence.utils.hibernate.type.basic.Inet;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class IpAddressDAOUnitTest extends AbstractDAOTest<IpAddressEntity> {
    IpAddressDAO ipAddressDAO;

    @BeforeEach
    public void setUp() {
        ipAddressDAO = new IpAddressDAO(new DPCAuthManagedSessionFactory(db.getSessionFactory()));
    }

    @Test
    void writesIpAddress() {
        UUID orgId = UUID.randomUUID();
        IpAddressEntity ipAddressEntity = createIpAddressEntity(orgId, "192.168.1.1", "test label");
        IpAddressEntity returnedIp = db.inTransaction(() -> ipAddressDAO.persistIpAddress(ipAddressEntity));

        assertEquals(orgId, returnedIp.getOrganizationId());
        assertEquals("192.168.1.1", returnedIp.getIpAddress().getAddress());
        assertEquals("test label", returnedIp.getLabel());
        assertFalse(returnedIp.getCreatedAt().toString().isEmpty());
        assertFalse(returnedIp.getId().toString().isEmpty());
    }

    @Test
    void failsWritingBadIpAddress() {
        IpAddressEntity ipAddressEntity = createIpAddressEntity(UUID.randomUUID(), "bad_ip", "test label");
        assertThrows(PersistenceException.class, () ->
            db.inTransaction(() -> ipAddressDAO.persistIpAddress(ipAddressEntity)));
    }

    @Test
    void fetchesIpAddress() {
        UUID orgId1 = UUID.randomUUID();
        UUID orgId2 = UUID.randomUUID();

        db.inTransaction(() -> {
            ipAddressDAO.persistIpAddress(createIpAddressEntity(orgId1, "192.168.1.1", "label 1"));
            ipAddressDAO.persistIpAddress(createIpAddressEntity(orgId1, "192.168.1.2", "label 2"));
            ipAddressDAO.persistIpAddress(createIpAddressEntity(orgId2, "192.168.1.3", "label 3"));
        });

        List<IpAddressEntity> results = db.inTransaction(() -> ipAddressDAO.fetchIpAddresses(orgId1));

        assertEquals(2, results.size());

        IpAddressEntity ip1 = results.get(0);
        assertEquals(orgId1, ip1.getOrganizationId());
        assertEquals("192.168.1.1", ip1.getIpAddress().getAddress());
        assertEquals("label 1", ip1.getLabel());

        IpAddressEntity ip2 = results.get(1);
        assertEquals(orgId1, ip2.getOrganizationId());
        assertEquals("192.168.1.2", ip2.getIpAddress().getAddress());
        assertEquals("label 2", ip2.getLabel());
    }

    @Test
    void deletesIpAddress() {
        UUID orgId = UUID.randomUUID();

        IpAddressEntity persistedIpAddress = db.inTransaction(() ->
            ipAddressDAO.persistIpAddress(createIpAddressEntity(orgId, "192.168.1.1", "label 1")));

        List<IpAddressEntity> results = db.inTransaction(() -> {
            ipAddressDAO.deleteIpAddress(persistedIpAddress);
            return ipAddressDAO.fetchIpAddresses(orgId);
        });

        assertEquals(0, results.size());
    }

    private IpAddressEntity createIpAddressEntity(UUID orgId, String address, String label) {
        return new IpAddressEntity()
            .setOrganizationId(orgId)
            .setIpAddress(new Inet(address))
            .setLabel(label);
    }
}
