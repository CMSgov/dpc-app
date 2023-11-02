package gov.cms.dpc.fhir.converters.entities;

import gov.cms.dpc.common.entities.EndpointEntity;
import gov.cms.dpc.common.entities.OrganizationEntity;
import gov.cms.dpc.fhir.converters.FHIREntityConverter;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Reference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class EndpointEntityConverterTest {
    EndpointEntityConverter entityConverter = new EndpointEntityConverter();
    FHIREntityConverter fhirEntityConverter = FHIREntityConverter.initialize();
    EndpointEntity endpointEntity;
    Endpoint endpoint;

    UUID uuid = UUID.randomUUID();
    UUID orgUuid = UUID.randomUUID();
    String name = "Endpoint Name";
    String address = "https://example.com/endpoint";
    String code = "code";
    String system = "system";

    @BeforeEach
    void buildEntities() {
        EndpointEntity.ConnectionType connectionType = new EndpointEntity.ConnectionType();
        OrganizationEntity organizationEntity = new OrganizationEntity();
        organizationEntity.setId(orgUuid);
        connectionType.setCode(code);
        connectionType.setSystem(system);
        endpointEntity = new EndpointEntity();
        endpointEntity.setId(uuid);
        endpointEntity.setName(name);
        endpointEntity.setAddress(address);
        endpointEntity.setOrganization(organizationEntity);
        endpointEntity.setStatus(Endpoint.EndpointStatus.ACTIVE);
        endpointEntity.setConnectionType(connectionType);

        endpoint = new Endpoint();
        endpoint.setId(new IdType("Endpoint", uuid.toString()));
        endpoint.setName(name);
        endpoint.setAddress(address);
        endpoint.setManagingOrganization(new Reference(new IdType("Organization", orgUuid.toString())));
        endpoint.setStatus(Endpoint.EndpointStatus.ACTIVE);
        endpoint.setConnectionType(fhirEntityConverter.toFHIR(Coding.class, connectionType));
    }

    @Test
    void fromFHIR() {
        EndpointEntity convertedEntity = entityConverter.fromFHIR(fhirEntityConverter, endpoint);
        assertEquals(uuid, convertedEntity.getId());
        assertEquals(name, convertedEntity.getName());
        assertEquals(address, convertedEntity.getAddress());
        assertEquals(orgUuid, convertedEntity.getOrganization().getId());
        assertEquals(endpoint.getStatus(), convertedEntity.getStatus());
        assertEquals(code, convertedEntity.getConnectionType().getCode());
        assertEquals(system, convertedEntity.getConnectionType().getSystem());
    }

    @Test
    void fromFHIR_noId() {
        endpoint.setId("");
        EndpointEntity convertedEntity = entityConverter.fromFHIR(fhirEntityConverter, endpoint);
        assertNotNull(convertedEntity.getId());
        assertTrue(convertedEntity.getId().toString().length() == uuid.toString().length());
    }

    @Test
    void fromFHIR_noOrg() {
        endpoint.setManagingOrganization(null);
        EndpointEntity convertedEntity = entityConverter.fromFHIR(fhirEntityConverter, endpoint);
        assertNotNull(convertedEntity.getOrganization());
        assertNull(convertedEntity.getOrganization().getId());
    }

    @Test
    void toFHIR() {
        Endpoint convertedResource = entityConverter.toFHIR(fhirEntityConverter, endpointEntity);
        assertEquals("Endpoint/" + endpointEntity.getId(), convertedResource.getId());
        assertEquals(name, convertedResource.getName());
        assertEquals(address, convertedResource.getAddress());
        assertEquals("Organization/" + orgUuid, convertedResource.getManagingOrganization().getReference());
        assertEquals(endpointEntity.getStatus(), convertedResource.getStatus());
        assertEquals(code, convertedResource.getConnectionType().getCode());
        assertEquals(system, convertedResource.getConnectionType().getSystem());
    }

    @Test
    void getFHIRResource() {
        assertEquals(Endpoint.class, entityConverter.getFHIRResource());
    }

    @Test
    void getJavaClass() {
        assertEquals(EndpointEntity.class, entityConverter.getJavaClass());
    }
}
