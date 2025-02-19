package gov.cms.dpc.api.cli.organizations;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.IRestfulClientFactory;
import ca.uhn.fhir.rest.gclient.IQuery;
import io.dropwizard.core.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class OrganizationListUnitTest {
	private final PrintStream originalOut = System.out;

	private final ByteArrayOutputStream stdOut = new ByteArrayOutputStream();

	@BeforeEach
	void cliSetup() {
		// Redirect stdout to our byte stream
		System.setOut(new PrintStream(stdOut));
	}

	@AfterEach
	void teardown() {
		System.setOut(originalOut);
	}

	@Test
	void test_constructor() {
		assertDoesNotThrow(OrganizationList::new);
	}

	@Test
	void test_addAdditionalOptions() {
		OrganizationList organizationList = new OrganizationList();
		Subparser subparser = mock(Subparser.class);
		assertDoesNotThrow(() -> organizationList.addAdditionalOptions(subparser));
	}

	@Test
	void test_run() {
		Bootstrap<?> bootstrap = mock(Bootstrap.class);
		Namespace namespace = new Namespace(Map.of("hostname", "fakeHost"));

		IGenericClient client = mock(IGenericClient.class, RETURNS_DEEP_STUBS);
		IRestfulClientFactory clientFactory = mock(IRestfulClientFactory.class);
		FhirContext ctx = mock(FhirContext.class);
		when(ctx.newRestfulGenericClient("fakeHost")).thenReturn(client);
		when(ctx.getRestfulClientFactory()).thenReturn(clientFactory);

		IQuery<IBaseBundle> iQueryBaseBundle = mock(IQuery.class);
		when(client
			.search()
			.forResource(Organization.class)
			.encodedJson()
		).thenReturn(iQueryBaseBundle);

		IQuery<Bundle> iQueryBundle = mock(IQuery.class);
		when(iQueryBaseBundle.returnBundle(Bundle.class)).thenReturn(iQueryBundle);

		Bundle bundle = mock(Bundle.class);
		when(iQueryBundle.execute()).thenReturn(bundle);

		Organization org = new Organization();
		org.setName("orgName");
		org.setId(UUID.randomUUID().toString());
		org.addIdentifier(new Identifier().setValue("NPI"));

		Bundle.BundleEntryComponent entryComponent = new Bundle.BundleEntryComponent();
		entryComponent.setResource(org);
		when(bundle.getEntry()).thenAnswer(answer -> List.of(entryComponent));

		try (MockedStatic<FhirContext> fhirCtx = mockStatic(FhirContext.class)) {
			fhirCtx.when(FhirContext::forDstu3).thenReturn(ctx);

			OrganizationList organizationList = new OrganizationList();
			organizationList.run(bootstrap, namespace);
		}

		String results = stdOut.toString();
		assertTrue(results.contains("orgName"));
		assertTrue(results.contains("NPI"));
		assertTrue(results.contains(org.getId()));
	}

}
