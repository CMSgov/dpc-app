package gov.cms.dpc.common.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import org.hl7.fhir.dstu3.model.Endpoint;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Endpoint Entity tests")
public class EndpointEntityTest {

	@Test
        @DisplayName("Test endpoint getters and setters 🥳")
	public void testGettersAndSetters() {
		EndpointEntity endpoint = new EndpointEntity();
		UUID id = UUID.randomUUID();
		OrganizationEntity org = new OrganizationEntity();
		Endpoint.EndpointStatus status = Endpoint.EndpointStatus.ACTIVE;
		EndpointEntity.ConnectionType connectionType = new EndpointEntity.ConnectionType();
		String name = "Test Endpoint";
		String address = "http://foo.bar/endpoint";
		EndpointEntity.ValidationStatus validationStatus = EndpointEntity.ValidationStatus.PENDING;
		String validationMessage = "In-progress";

		endpoint.setId(id);
		endpoint.setOrganization(org);
		endpoint.setStatus(status);
		endpoint.setConnectionType(connectionType);
		endpoint.setName(name);
		endpoint.setAddress(address);
		endpoint.setValidationStatus(validationStatus);
		endpoint.setValidationMessage(validationMessage);

		assertEquals(id, endpoint.getId());
		assertEquals(org, endpoint.getOrganization());
		assertEquals(status, endpoint.getStatus());
		assertEquals(connectionType, endpoint.getConnectionType());
		assertEquals(name, endpoint.getName());
		assertEquals(address, endpoint.getAddress());
		assertEquals(validationStatus, endpoint.getValidationStatus());
		assertEquals(validationMessage, endpoint.getValidationMessage());

	}

	@Test
        @DisplayName("Update endpoint 🥳")
	public void testUpdate() {
		EndpointEntity e1 = new EndpointEntity();
		EndpointEntity e2 = new EndpointEntity();

		e1.setName("Test");

		e2.update(e1);
		assertEquals(e1.getName(), e2.getName());
	}

	@Test
        @DisplayName("Test connection getters and setters 🥳")
	public void testConnectionGettersAndSetters() {
		EndpointEntity.ConnectionType connectionType = new EndpointEntity.ConnectionType();
		String system = "example-system";
		String code = "example-code";

		connectionType.setSystem(system);
		connectionType.setCode(code);

		assertEquals(system, connectionType.getSystem());
		assertEquals(code, connectionType.getCode());
	}

}
