package gov.cms.dpc.api.resources.v1;

import gov.cms.dpc.api.APITestHelpers;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.entities.IpAddressEntity;
import gov.cms.dpc.api.jdbi.IpAddressDAO;
import gov.cms.dpc.api.models.CollectionResponse;
import gov.cms.dpc.api.models.CreateIpAddressRequest;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IpAddressResourceUnitTest {
    IpAddressResource ipAddressResource;
    OrganizationPrincipal organizationPrincipal = APITestHelpers.makeOrganizationPrincipal();

    @Mock
    IpAddressDAO ipAddressDAO;

    @BeforeEach
    public void setup() {
        ipAddressResource = new IpAddressResource(ipAddressDAO);
    }

    @Test
    public void testGet() {
        IpAddressEntity ipAddressEntity = new IpAddressEntity();

        when(ipAddressDAO.fetchIpAddresses(organizationPrincipal.getID())).thenReturn(List.of(ipAddressEntity));

        CollectionResponse response = ipAddressResource.getOrganizationIpAddresses(organizationPrincipal);
        assertEquals(1, response.getCount());
        assertTrue(response.getEntities().contains(ipAddressEntity));
    }

    @Test
    public void testGet_nothingReturned() {
        when(ipAddressDAO.fetchIpAddresses(organizationPrincipal.getID())).thenReturn(List.of());

        CollectionResponse response = ipAddressResource.getOrganizationIpAddresses(organizationPrincipal);
        assertEquals(0, response.getCount());
    }

    @Test
    public void testPost_happyPath() {
        CreateIpAddressRequest createIpAddressRequest = new CreateIpAddressRequest("192.168.1.1");
        IpAddressEntity ipAddressEntity = new IpAddressEntity();

        when(ipAddressDAO.fetchIpAddresses(organizationPrincipal.getID())).thenReturn(List.of());
        when(ipAddressDAO.persistIpAddress(any())).thenReturn(ipAddressEntity);

        IpAddressEntity response = ipAddressResource.submitIpAddress(organizationPrincipal, createIpAddressRequest);
        assertSame(ipAddressEntity, response);
    }

    @Test
    public void testPost_badIp() {
        CreateIpAddressRequest createIpAddressRequest = new CreateIpAddressRequest("1.bad.ip.addr");
        IpAddressEntity ipAddressEntity = new IpAddressEntity();

        assertThrows(WebApplicationException.class, () -> {
            ipAddressResource.submitIpAddress(organizationPrincipal, createIpAddressRequest);
        });
    }

    @Test
    public void testPost_tooManyIps() {
        CreateIpAddressRequest createIpAddressRequest = new CreateIpAddressRequest("192.168.1.1");

        List<IpAddressEntity> existingIps = new ArrayList<>();
        for(int i=0; i <= 8; i++) {
            existingIps.add(new IpAddressEntity());
        }

        when(ipAddressDAO.fetchIpAddresses(organizationPrincipal.getID())).thenReturn(existingIps);

        assertThrows(WebApplicationException.class, () -> {
            ipAddressResource.submitIpAddress(organizationPrincipal, createIpAddressRequest);
        });
    }

    @Test
    public void testDelete_happyPath() {
        UUID ipId = UUID.randomUUID();
        IpAddressEntity existingIp = new IpAddressEntity().setId(ipId);

        when(ipAddressDAO.fetchIpAddresses(organizationPrincipal.getID())).thenReturn(List.of(existingIp));

        Response response = ipAddressResource.deleteIpAddress(organizationPrincipal, ipId);
        assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatus());
    }

    @Test
    public void testDelete_notFound() {
        IpAddressEntity existingIp = new IpAddressEntity().setId(UUID.randomUUID());

        when(ipAddressDAO.fetchIpAddresses(organizationPrincipal.getID())).thenReturn(List.of(existingIp));

        assertThrows(WebApplicationException.class, () -> {
            ipAddressResource.deleteIpAddress(organizationPrincipal, UUID.randomUUID());
        });
    }
}