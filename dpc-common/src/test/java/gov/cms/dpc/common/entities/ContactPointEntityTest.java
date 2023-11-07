package gov.cms.dpc.common.entities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.hl7.fhir.dstu3.model.ContactPoint;
import org.junit.Test;
import java.util.UUID;

public class ContactPointEntityTest {


	@Test
	public void testGettersAndSetters() {
		ContactPointEntity contactPoint = new ContactPointEntity();
		UUID id = UUID.randomUUID();
        ContactEntity contact = new ContactEntity();
		ContactPoint.ContactPointSystem system = ContactPoint.ContactPointSystem.EMAIL;
        ContactPoint.ContactPointUse use = ContactPoint.ContactPointUse.WORK;
        String value = "foo@bar.com";
        Integer rank = 1;

		contactPoint.setId(id);
        contactPoint.setContactEntity(contact);
        contactPoint.setSystem(system);
        contactPoint.setUse(use);
        contactPoint.setValue(value);
        contactPoint.setRank(rank);


		assertEquals(id, contactPoint.getId());
        assertEquals(contact, contactPoint.getContactEntity());
        assertEquals(system, contactPoint.getSystem());
        assertEquals(use, contactPoint.getUse());
        assertEquals(value, contactPoint.getValue());
        assertEquals(rank, contactPoint.getRank());

	}

	@Test
	public void testToFHIR() {
        ContactPointEntity contactPoint = new ContactPointEntity();
        ContactPoint.ContactPointSystem system = ContactPoint.ContactPointSystem.EMAIL;
        ContactPoint.ContactPointUse use = ContactPoint.ContactPointUse.WORK;
        String value = "foo@bar.com";

        contactPoint.setSystem(system);
        contactPoint.setUse(use);
        contactPoint.setValue(value);

        ContactPoint fhirContactPoint = contactPoint.toFHIR();
        
        assertNotNull(fhirContactPoint);
        assertEquals(system, fhirContactPoint.getSystem());
        assertEquals(use, fhirContactPoint.getUse());
        assertEquals(value, fhirContactPoint.getValue());


	}
}
