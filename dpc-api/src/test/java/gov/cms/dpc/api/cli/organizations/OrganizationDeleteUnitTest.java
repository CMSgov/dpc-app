package gov.cms.dpc.api.cli.organizations;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.IRestfulClientFactory;
import ca.uhn.fhir.rest.gclient.IDeleteTyped;
import io.dropwizard.core.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Argument;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.hl7.fhir.dstu3.model.IdType;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Map;

import static org.mockito.Mockito.*;

class OrganizationDeleteUnitTest {
	@Test
	void test_addAdditionalOptions() {
		OrganizationDelete organizationDelete = new OrganizationDelete();

		Subparser subparser = mock(Subparser.class);
		Argument argument = mock(Argument.class);

		when(subparser.addArgument("id")).thenReturn(argument);
		when(argument.required(true)).thenReturn(argument);
		when(argument.dest("org-reference")).thenReturn(argument);
		when(argument.help("ID of Organization to delete")).thenReturn(argument);

		organizationDelete.addAdditionalOptions(subparser);

		verify(subparser).addArgument("id");
		verify(argument).required(true);
		verify(argument).dest("org-reference");
		verify(argument).help("ID of Organization to delete");
	}

	@Test
	void test_run() {
		Bootstrap<?> bootstrap = mock(Bootstrap.class);
		Namespace namespace = new Namespace(Map.of("org-reference", "fakeOrg", "hostname", "fakeHost"));

		IGenericClient client = mock(IGenericClient.class, RETURNS_DEEP_STUBS);
		IRestfulClientFactory clientFactory = mock(IRestfulClientFactory.class);
		FhirContext ctx = mock(FhirContext.class);
		when(ctx.newRestfulGenericClient("fakeHost")).thenReturn(client);
		when(ctx.getRestfulClientFactory()).thenReturn(clientFactory);

		IDeleteTyped deleteTyped = mock(IDeleteTyped.class);
		when(client
			.delete()
			.resourceById(new IdType("fakeOrg"))
			.encodedJson()
		).thenReturn(deleteTyped);

		try (MockedStatic<FhirContext> fhirCtx = mockStatic(FhirContext.class)) {
			fhirCtx.when(FhirContext::forDstu3).thenReturn(ctx);

			OrganizationDelete organizationDelete = new OrganizationDelete();
			organizationDelete.run(bootstrap, namespace);

			verify(deleteTyped).execute();
		}
	}
}
