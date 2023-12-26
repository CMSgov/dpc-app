package gov.cms.dpc.api.jdbi;

import gov.cms.dpc.api.entities.IpAddressEntity;
import gov.cms.dpc.api.entities.IpAddressEntity_;
import gov.cms.dpc.common.hibernate.auth.DPCAuthManagedSessionFactory;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.hypersistence.utils.hibernate.type.basic.Inet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DropwizardExtensionsSupport.class)
@Testcontainers
class IpAddressDAOTest extends AbstractDAOTest {
    IpAddressDAO ipAddressDAO;

    @BeforeEach
    public void setUp() {
        ipAddressDAO = new IpAddressDAO(new DPCAuthManagedSessionFactory(db.getSessionFactory()));
    }

    @Test
    public void writesIpAddress() {
        IpAddressEntity ipAddressEntity = createIpAddressEntity(UUID.randomUUID(), "192.168.1.1", "test label");

        Optional<IpAddressEntity> optionalResult = db.inTransaction(() -> ipAddressDAO.persistIpAddress(ipAddressEntity));
        assertFalse(optionalResult.isEmpty());

        IpAddressEntity returnedIp = optionalResult.get();
        assertEquals("192.168.1.1", returnedIp.getIpAddress().getAddress());
        assertEquals("test label", returnedIp.getLabel());
        assertFalse(returnedIp.getCreatedAt().toString().isEmpty());
        assertFalse(returnedIp.getIpAddressId().toString().isEmpty());
        assertFalse(returnedIp.getOrganizationId().toString().isEmpty());
    }

    @Test
    public void fetchesIpAddress() {
        UUID orgId1 = UUID.randomUUID();
        UUID orgId2 = UUID.randomUUID();

        List<IpAddressEntity> results = db.inTransaction(() -> {
            ipAddressDAO.persistIpAddress(createIpAddressEntity(orgId1, "192.168.1.1", "label 1"));
            ipAddressDAO.persistIpAddress(createIpAddressEntity(orgId1, "192.168.1.2", "label 2"));
            ipAddressDAO.persistIpAddress(createIpAddressEntity(orgId2, "192.168.1.3", "label 3"));

            return ipAddressDAO.fetchIpAddresses(orgId1);
        });

        assertEquals(2, results.size());

        IpAddressEntity org1 = results.get(0);
        assertEquals(orgId1, org1.getOrganizationId());
        assertEquals("192.168.1.1", org1.getIpAddress().getAddress());
        assertEquals("label 1", org1.getLabel());

        IpAddressEntity org2 = results.get(1);
        assertEquals(orgId1, org2.getOrganizationId());
        assertEquals("192.168.1.2", org2.getIpAddress().getAddress());
        assertEquals("label 2", org2.getLabel());
    }

    @Test
    public void deletesIpAddress() {
        UUID orgId = UUID.randomUUID();

        List<IpAddressEntity> results = db.inTransaction(() -> {
            IpAddressEntity persistedIpAddress =
                ipAddressDAO.persistIpAddress(createIpAddressEntity(orgId, "192.168.1.1", "label 1")).get();

            ipAddressDAO.deleteIpAddress(persistedIpAddress);

            return ipAddressDAO.fetchIpAddresses(orgId);
        });

        assertEquals(0, results.size());
    }

    private IpAddressEntity createIpAddressEntity(UUID orgId, String address, String label) {
        IpAddressEntity ip = new IpAddressEntity();
        ip.setOrganizationId(orgId);
        ip.setIpAddress(new Inet(address));
        ip.setLabel(label);
        return ip;
    }
}