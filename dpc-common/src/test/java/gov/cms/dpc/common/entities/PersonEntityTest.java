package gov.cms.dpc.common.entities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.Test;
import java.time.OffsetDateTime;
import java.util.UUID;

public class PersonEntityTest {

	private PersonEntityImpl personEntity;

	@Before
	public void setup() {
		this.personEntity = new PersonEntityImpl();
	}

	@Test
	public void testGettersAndSetters() {
		String firstName = "Sydney";
		String lastName = "Danger";
		UUID id = UUID.randomUUID();
		OffsetDateTime createdUpdatedDateTime = OffsetDateTime.now();

		personEntity.setFirstName(firstName);
		personEntity.setLastName(lastName);
		personEntity.setID(id);
		personEntity.setCreatedAt(createdUpdatedDateTime);
		personEntity.setUpdatedAt(createdUpdatedDateTime);

		assertEquals(firstName, personEntity.getFirstName());
		assertEquals(lastName, personEntity.getLastName());
		assertEquals(id, personEntity.getID());
		assertEquals(createdUpdatedDateTime, personEntity.getCreatedAt());
		assertEquals(createdUpdatedDateTime, personEntity.getUpdatedAt());
	}

	@Test
	public void testSetCreation() {
		personEntity.setCreation();
		assertNotNull(personEntity.getCreatedAt());
		assertNotNull(personEntity.getUpdatedAt());
	}
	@Test
	public void testSetUpdateTime() {
		personEntity.setUpdateTime();
		assertNotNull(personEntity.getUpdatedAt());
	}

}

class PersonEntityImpl extends PersonEntity{
	public PersonEntityImpl(){
		super();
	}
	public PersonEntityImpl(String firstName, String lastName){
		super();
		setFirstName(firstName);
		setLastName(lastName);
	}
}