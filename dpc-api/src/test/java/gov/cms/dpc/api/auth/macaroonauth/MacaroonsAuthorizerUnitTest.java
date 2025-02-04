package gov.cms.dpc.api.auth.macaroonauth;

import gov.cms.dpc.api.auth.OrganizationPrincipal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class MacaroonsAuthorizerUnitTest {
	@Test
	void test_authorize_works() {
		MacaroonsAuthorizer authorizer = new MacaroonsAuthorizer() {};
		OrganizationPrincipal principal = mock(OrganizationPrincipal.class);
		String role = "role";

		assertTrue(authorizer.authorize(principal, role, null));
	}

}
