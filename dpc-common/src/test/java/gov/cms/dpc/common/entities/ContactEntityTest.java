package gov.cms.dpc.common.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.hl7.fhir.dstu3.model.Organization;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Contact Entity tests")
public class ContactEntityTest {

	@Test
	@DisplayName("Test contact getters and setters 🥳")
public void testGettersAndSetters() {
		ContactEntity contact = new ContactEntity();
		UUID id = UUID.randomUUID();
		OrganizationEntity org = new OrganizationEntity();
		NameEntity name = new NameEntity();
		List<ContactPointEntity> telecom = new ArrayList<>();
		AddressEntity address = new AddressEntity();

		contact.setId(id);
		contact.setOrganization(org);
		contact.setName(name);
		contact.setTelecom(telecom);
		contact.setAddress(address);

		assertEquals(id, contact.getId());
		assertEquals(org, contact.getOrganization());
		assertEquals(name, contact.getName());
		assertEquals(telecom, contact.getTelecom());
		assertEquals(address, contact.getAddress());

	}

	@Test
	@DisplayName("Convert contact to FHIR 🥳")
public void testToFHIR() {
		ContactEntity contact = new ContactEntity();
		NameEntity name = new NameEntity();
		name.setFamily("Dog");
		List<ContactPointEntity> telecom = new ArrayList<>();
		AddressEntity address = new AddressEntity();

		contact.setName(name);
		contact.setTelecom(telecom);
		contact.setAddress(address);

		Organization.OrganizationContactComponent fhirContact = contact.toFHIR();

		assertNotNull(fhirContact);

		assertEquals(name.getFamily(), fhirContact.getName().getFamily());
		assertEquals(telecom.size(), fhirContact.getTelecom().size());
		assertEquals(address.getType(), fhirContact.getAddress().getType());

	}
}
