package gov.cms.dpc.api.auth.macaroonauth;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import gov.cms.dpc.api.auth.DPCAuthCredentials;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.DPCResourceType;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class MacaroonsAuthenticatorUnitTest {
	private IGenericClient client = mock(IGenericClient.class, RETURNS_DEEP_STUBS);
	private PathAuthorizer pa = mock(PathAuthorizer.class);
	private Organization org = new Organization();
	private MacaroonsAuthenticator macaroonsAuthenticator = new MacaroonsAuthenticator(client);

	@Test
	void test_authenticate_no_path_authorizer() {
		DPCAuthCredentials dpcAuthCredentials = new DPCAuthCredentials("macaroon", org, null, "path");
		OrganizationPrincipal principal = macaroonsAuthenticator.authenticate(dpcAuthCredentials).get();
		assertSame(org, principal.getOrganization());
	}

	@Test
	void test_authenticate_org_id_passes() {
		when(pa.type()).thenReturn(DPCResourceType.Organization);
		String orgIdBase = UUID.randomUUID().toString();
		org.setId("Organization/" + orgIdBase);

		DPCAuthCredentials dpcAuthCredentials = new DPCAuthCredentials("macaroon", org, pa, orgIdBase);
		OrganizationPrincipal principal = macaroonsAuthenticator.authenticate(dpcAuthCredentials).get();
		assertSame(org, principal.getOrganization());
	}

	@Test
	void test_authenticate_org_id_fails() {
		when(pa.type()).thenReturn(DPCResourceType.Organization);
		String orgIdBase = UUID.randomUUID().toString();
		org.setId("Organization/" + orgIdBase);

		DPCAuthCredentials dpcAuthCredentials = new DPCAuthCredentials("macaroon", org, pa, "fakeId");
		assertTrue(macaroonsAuthenticator.authenticate(dpcAuthCredentials).isEmpty());
	}

	@Test
	void test_authenticate_resource_authorized() {
		when(pa.type()).thenReturn(DPCResourceType.Patient);
		String orgIdBase = UUID.randomUUID().toString();
		org.setId("Organization/" + orgIdBase);

		DPCAuthCredentials dpcAuthCredentials = new DPCAuthCredentials("macaroon", org, pa, "fakeId");
		Map<String, List<String>> searchParams = new HashMap<>();
		searchParams.put("_id", Collections.singletonList(dpcAuthCredentials.getPathValue()));
		searchParams.put("organization", Collections.singletonList(dpcAuthCredentials.getOrganization().getId()));

		IQuery<IBaseBundle> searchQuery = mock(IQuery.class);
		when(client
			.search()
			.forResource(DPCResourceType.Patient.name())
		).thenReturn(searchQuery);

		IQuery<IBaseBundle> whereMapQuery = mock(IQuery.class, RETURNS_DEEP_STUBS);
		when(searchQuery.whereMap(searchParams)).thenReturn(whereMapQuery);

		IQuery<Bundle> bundleQuery = mock(IQuery.class);
		Bundle bundle = mock(Bundle.class);
		when(bundle.getTotal()).thenReturn(1);
		when(whereMapQuery.returnBundle(Bundle.class).encodedJson()).thenReturn(bundleQuery);
		when(bundleQuery.execute()).thenReturn(bundle);

		assertSame(org, macaroonsAuthenticator.authenticate(dpcAuthCredentials).get().getOrganization());
	}

	@Test
	void test_authenticate_resource_not_authorized() {
		when(pa.type()).thenReturn(DPCResourceType.Patient);
		String orgIdBase = UUID.randomUUID().toString();
		org.setId("Organization/" + orgIdBase);

		DPCAuthCredentials dpcAuthCredentials = new DPCAuthCredentials("macaroon", org, pa, "fakeId");
		Map<String, List<String>> searchParams = new HashMap<>();
		searchParams.put("_id", Collections.singletonList(dpcAuthCredentials.getPathValue()));
		searchParams.put("organization", Collections.singletonList(dpcAuthCredentials.getOrganization().getId()));

		IQuery<IBaseBundle> searchQuery = mock(IQuery.class);
		when(client
			.search()
			.forResource(DPCResourceType.Patient.name())
		).thenReturn(searchQuery);

		IQuery<IBaseBundle> whereMapQuery = mock(IQuery.class, RETURNS_DEEP_STUBS);
		when(searchQuery.whereMap(searchParams)).thenReturn(whereMapQuery);

		IQuery<Bundle> bundleQuery = mock(IQuery.class);
		Bundle bundle = mock(Bundle.class);
		when(bundle.getTotal()).thenReturn(0);
		when(whereMapQuery.returnBundle(Bundle.class).encodedJson()).thenReturn(bundleQuery);
		when(bundleQuery.execute()).thenReturn(bundle);

		assertTrue(macaroonsAuthenticator.authenticate(dpcAuthCredentials).isEmpty());
	}

	@Test
	void test_authenticate_resource_group() {
		when(pa.type()).thenReturn(DPCResourceType.Group);
		String orgIdBase = UUID.randomUUID().toString();
		org.setId("Organization/" + orgIdBase);

		DPCAuthCredentials dpcAuthCredentials = new DPCAuthCredentials("macaroon", org, pa, "fakeId");
		Map<String, List<String>> searchParams = new HashMap<>();
		searchParams.put("_id", Collections.singletonList(dpcAuthCredentials.getPathValue()));
		searchParams.put("organization", Collections.singletonList(dpcAuthCredentials.getOrganization().getId()));
		searchParams.put("_tag", Collections.singletonList(String.format("%s|%s", DPCIdentifierSystem.DPC.getSystem(), dpcAuthCredentials.getOrganization().getId())));

		IQuery<IBaseBundle> searchQuery = mock(IQuery.class);
		when(client
			.search()
			.forResource(DPCResourceType.Group.name())
		).thenReturn(searchQuery);

		IQuery<IBaseBundle> whereMapQuery = mock(IQuery.class, RETURNS_DEEP_STUBS);
		when(searchQuery.whereMap(searchParams)).thenReturn(whereMapQuery);

		IQuery<Bundle> bundleQuery = mock(IQuery.class);
		Bundle bundle = mock(Bundle.class);
		when(bundle.getTotal()).thenReturn(1);
		when(whereMapQuery.returnBundle(Bundle.class).encodedJson()).thenReturn(bundleQuery);
		when(bundleQuery.execute()).thenReturn(bundle);

		assertSame(org, macaroonsAuthenticator.authenticate(dpcAuthCredentials).get().getOrganization());
	}
}
