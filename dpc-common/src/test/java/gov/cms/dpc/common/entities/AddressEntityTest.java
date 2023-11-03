package gov.cms.dpc.common.entities;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import gov.cms.dpc.common.Constants;
import org.hl7.fhir.dstu3.model.Address;

public class AddressEntityTest {

	@Test
	public void testGettersAndSetters() {
		//Create an object, and set / get the objects in the Entity.
		AddressEntity address = new AddressEntity();

        address.setCity(Constants.ADDRESS_ENTITY_CITY);
        address.setCountry(Constants.ADDRESS_ENTITY_COUNTRY);
        address.setDistrict(Constants.ADDRESS_ENTITY_DISTRICT);
        address.setPostalCode(Constants.ADDRESS_ENTITY_ZIP);
        address.setState(Constants.ADDRESS_ENTITY_STATE);
        address.setUse(Address.AddressUse.WORK);
        address.setType(Address.AddressType.BOTH);
		address.setLine1(new String(Constants.ADDRESS_ENTITY_LINE1));
		address.setLine2(new String(Constants.ADDRESS_ENTITY_LINE2));

		assertEquals(Constants.ADDRESS_ENTITY_CITY, address.getCity());
		assertEquals(Constants.ADDRESS_ENTITY_COUNTRY, address.getCountry());
		assertEquals(Constants.ADDRESS_ENTITY_DISTRICT, address.getDistrict());
		assertEquals(Constants.ADDRESS_ENTITY_LINE1, address.getLine1());
		assertEquals(Constants.ADDRESS_ENTITY_LINE2, address.getLine2());
		assertEquals(Constants.ADDRESS_ENTITY_ZIP, address.getPostalCode());
		assertEquals(Constants.ADDRESS_ENTITY_STATE, address.getState());

	}

	@Test
	public void testToFHIR(){
		AddressEntity address = new AddressEntity();

        address.setCity(Constants.ADDRESS_ENTITY_CITY);
        address.setCountry(Constants.ADDRESS_ENTITY_COUNTRY);
        address.setDistrict(Constants.ADDRESS_ENTITY_DISTRICT);
        address.setPostalCode(Constants.ADDRESS_ENTITY_ZIP);
        address.setState(Constants.ADDRESS_ENTITY_STATE);
        address.setUse(Address.AddressUse.WORK);
        address.setType(Address.AddressType.BOTH);
		address.setLine1(new String(Constants.ADDRESS_ENTITY_LINE1));
		address.setLine2(new String(Constants.ADDRESS_ENTITY_LINE2));

		Address address2 = address.toFHIR();

		assertEquals(Constants.ADDRESS_ENTITY_CITY, address2.getCity());
		assertEquals(Constants.ADDRESS_ENTITY_COUNTRY, address2.getCountry());
		assertEquals(Constants.ADDRESS_ENTITY_DISTRICT, address2.getDistrict());
		String l1 = address2.getLine().get(0).toString();
		String l2 = address2.getLine().get(1).toString();

		assertEquals(Constants.ADDRESS_ENTITY_LINE1, l1);
		assertEquals(Constants.ADDRESS_ENTITY_LINE2, l2);
		assertEquals(Constants.ADDRESS_ENTITY_ZIP, address2.getPostalCode());
		assertEquals(Constants.ADDRESS_ENTITY_STATE, address2.getState());


	}


}
