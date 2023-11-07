package gov.cms.dpc.common.entities;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import org.hl7.fhir.dstu3.model.Address;

public class AddressEntityTest {
	private String addressCity = "Galveston";
    private String addressCountry = "US";
    private String addressDistrict = "Southern";
    private String addressZip = "77550";
    private String addressState = "TX";
    private String addressLine1 = "416 21st St";
    private String addressLine2 = "N/A";

	@Test
	public void testGettersAndSetters() {
		//Create an object, and set / get the objects in the Entity.
		AddressEntity address = new AddressEntity();
        address.setCity(addressCity);
        address.setCountry(addressCountry);
        address.setDistrict(addressDistrict);
        address.setPostalCode(addressZip);
        address.setState(addressState);
        address.setUse(Address.AddressUse.WORK);
        address.setType(Address.AddressType.BOTH);
		address.setLine1(addressLine1);
		address.setLine2(addressLine2);

		assertEquals(addressCity, address.getCity());
		assertEquals(addressCountry, address.getCountry());
		assertEquals(addressDistrict, address.getDistrict());
		assertEquals(addressLine1, address.getLine1());
		assertEquals(addressLine2, address.getLine2());
		assertEquals(addressZip, address.getPostalCode());
		assertEquals(addressState, address.getState());

	}

	@Test
	public void testToFHIR(){
		AddressEntity address = new AddressEntity();

        address.setCity(addressCity);
        address.setCountry(addressCountry);
        address.setDistrict(addressDistrict);
        address.setPostalCode(addressZip);
        address.setState(addressState);
        address.setUse(Address.AddressUse.WORK);
        address.setType(Address.AddressType.BOTH);
		address.setLine1(addressLine1);
		address.setLine2(addressLine2);

		Address address2 = address.toFHIR();

		assertEquals(addressCity, address2.getCity());
		assertEquals(addressCountry, address2.getCountry());
		assertEquals(addressDistrict, address2.getDistrict());
		String l1 = address2.getLine().get(0).toString();
		String l2 = address2.getLine().get(1).toString();

		assertEquals(addressLine1, l1);
		assertEquals(addressLine2, l2);
		assertEquals(addressZip, address2.getPostalCode());
		assertEquals(addressState, address2.getState());


	}


}
