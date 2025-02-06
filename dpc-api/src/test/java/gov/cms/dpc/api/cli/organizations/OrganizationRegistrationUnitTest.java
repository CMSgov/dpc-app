package gov.cms.dpc.api.cli.organizations;

import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.Argument;
import net.sourceforge.argparse4j.inf.Subparser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class OrganizationRegistrationUnitTest {
	@Test
	void test_constructor() {
		assertDoesNotThrow(OrganizationRegistration::new);
	}

	@Test
	void test_addAdditionalOptions() {
		OrganizationRegistration organizationRegistration = new OrganizationRegistration();

		Subparser subparser = mock(Subparser.class);
		Argument argument = mock(Argument.class);

		when(subparser.addArgument("-f", "--file")).thenReturn(argument);
		when(argument.dest(anyString())).thenReturn(argument);
		when(argument.type(any(Class.class))).thenReturn(argument);
		when(argument.required(true)).thenReturn(argument);
		when(argument.help(anyString())).thenReturn(argument);

		when(subparser.addArgument("--no-token")).thenReturn(argument);
		when(argument.action(Arguments.storeTrue())).thenReturn(argument);

		when(subparser.addArgument("-a", "--api")).thenReturn(argument);
		when(argument.setDefault("http://localhost:9911/tasks")).thenReturn(argument);

		// Verify the final call of each chain
		organizationRegistration.addAdditionalOptions(subparser);
		verify(argument, times(3)).help(anyString());
	}
}
