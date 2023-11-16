package gov.cms.dpc.common.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

import org.hl7.fhir.dstu3.model.HumanName;

public class NameEntityTest {

	@Test
	public void testGettersAndSetters() {
		NameEntity name = new NameEntity();
		HumanName.NameUse use = HumanName.NameUse.OFFICIAL;
		String given = "Australian";
		String family = "Shepherd";
		String prefix = "Dr.";
		String suffix = "III";

		name.setUse(use);
		name.setGiven(given);
		name.setFamily(family);
		name.setPrefix(prefix);
		name.setSuffix(suffix);

		assertEquals(use, name.getUse());
		assertEquals(given, name.getGiven());
		assertEquals(family, name.getFamily());
		assertEquals(prefix, name.getPrefix());
		assertEquals(suffix, name.getSuffix());
	}

	@Test
	public void testToFHIR() {
		NameEntity name = new NameEntity();
		HumanName.NameUse use = HumanName.NameUse.OFFICIAL;
		String given = "Australian";
		String family = "Shepherd";
		String prefix = "Dr.";
		String suffix = "III";

		name.setUse(use);
		name.setGiven(given);
		name.setFamily(family);
		name.setPrefix(prefix);
		name.setSuffix(suffix);

		HumanName fhirName = name.toFHIR();

		assertNotNull(fhirName);
		assertEquals(use, fhirName.getUse());
		assertEquals(given, fhirName.getGiven().get(0).getValue());
		assertEquals(family, fhirName.getFamily());
		assertEquals(prefix, fhirName.getPrefix().get(0).getValue());
		assertEquals(suffix, fhirName.getSuffix().get(0).getValue());
	}

}
