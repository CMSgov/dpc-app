package gov.cms.dpc.api.cli.organizations;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrganizationCommandUnitTest {
	@Test
	void test_constructor() {
		OrganizationCommand orgCommand = new OrganizationCommand();
		assertEquals("organization", orgCommand.getName());
	}
}
