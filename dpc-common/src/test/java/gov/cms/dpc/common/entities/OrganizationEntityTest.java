
package gov.cms.dpc.common.entities;

import org.junit.Test;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import org.hl7.fhir.dstu3.model.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class OrganizationEntityTest {

	@Test
	public void testGettersAndSetters() {
		OrganizationEntity org = new OrganizationEntity();
		UUID id = UUID.randomUUID();
		OrganizationEntity.OrganizationID organizationID = new OrganizationEntity.OrganizationID(DPCIdentifierSystem.NPPES,"1234");
		String orgName = "CMS";
		AddressEntity orgAddress = new AddressEntity();
		List<ContactEntity> contacts = new ArrayList<>();
		List<EndpointEntity> endpointEntities = new ArrayList<>();
		List<ProviderEntity> providerEntities = new ArrayList<>();
		List<PatientEntity> patientEntities = new ArrayList<>();
		List<RosterEntity> rosters = new ArrayList<>();

		org.setId(id);
		org.setOrganizationID(organizationID);
		org.setOrganizationName(orgName);
		org.setOrganizationAddress(orgAddress);
		org.setContacts(contacts);
		org.setEndpoints(endpointEntities);
		org.setProviders(providerEntities);
		org.setPatients(patientEntities);
		org.setRosters(rosters);

		assertEquals(id, org.getId());
		assertEquals(organizationID, org.getOrganizationID());
		assertEquals(orgName, org.getOrganizationName());
		assertEquals(orgAddress, org.getOrganizationAddress());
		assertEquals(contacts, org.getContacts());
		assertEquals(endpointEntities, org.getEndpoints());
		assertEquals(providerEntities, org.getProviders());
		assertEquals(patientEntities, org.getPatients());
		assertEquals(rosters, org.getRosters());
	
	}

	@Test
	public void testUpdate() {
		OrganizationEntity o1 = new OrganizationEntity();
		OrganizationEntity o2 = new OrganizationEntity();
		o1.setOrganizationName("Test-Entity");
		o2.update(o1);

		assertEquals(o1.getOrganizationName(), o2.getOrganizationName());
	}

	@Test
	public void testOrganizationIDGettersAndSetters() {
		OrganizationEntity.OrganizationID orgId = new OrganizationEntity.OrganizationID();
		DPCIdentifierSystem system = DPCIdentifierSystem.NPPES;
		String val = "1234";

		orgId.setSystem(system);
		orgId.setValue(val);

		assertEquals(system, orgId.getSystem());
		assertEquals(val, orgId.getValue());
	}

	@Test
	public void testOrganizationIDToFHIR() {
		OrganizationEntity.OrganizationID orgId = new OrganizationEntity.OrganizationID(DPCIdentifierSystem.NPPES,"1234");
		Identifier fhirID = orgId.toFHIR();

		assertEquals(DPCIdentifierSystem.NPPES.getSystem(), fhirID.getSystem());
		assertEquals("1234", fhirID.getValue());
	}
}
