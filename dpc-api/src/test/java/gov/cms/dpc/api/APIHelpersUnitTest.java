package gov.cms.dpc.api;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IOperationUntypedWithInput;
import gov.cms.dpc.testing.factories.BundleFactory;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;

import javax.ws.rs.WebApplicationException;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

class APIHelpersUnitTest {
	@Mock
	private Function<Patient, Optional<WebApplicationException>> entryFunction;

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private IGenericClient client;

	@BeforeEach
	public void setUp() {
		openMocks(this);
	}

	@Test
	void test_bulkResourceClient_happy_path() {
		// If the patients are completely empty they fail the hasResource() filter
		Bundle bundle = BundleFactory.createBundle(
			new Patient().setId(UUID.randomUUID().toString()),
			new Patient().setId(UUID.randomUUID().toString())
		);

		when(entryFunction.apply(any())).thenReturn(Optional.empty());

		IOperationUntypedWithInput<Bundle> bundleOperation = mock(IOperationUntypedWithInput.class);
		when(client
			.operation()
			.onType(Patient.class)
			.named("submit")
			.withParameters(any())
			.returnResourceType(Bundle.class)
			.encodedJson()
		).thenReturn(bundleOperation);
		when(bundleOperation.execute()).thenReturn(bundle);

		Bundle returnBundle = APIHelpers.bulkResourceClient(Patient.class, client, entryFunction, bundle);

		Mockito.verify(entryFunction, times(2)).apply(any());
		assertEquals(bundle, returnBundle);
	}

	@Test
	void test_bulkResourceClient_fails_validation() {
		Bundle bundle = BundleFactory.createBundle(
			new Patient().setId(UUID.randomUUID().toString()),
			new Patient().setId(UUID.randomUUID().toString())
		);

		when(entryFunction.apply(any())).thenReturn(Optional.of(new WebApplicationException("failed validation")));

		WebApplicationException exception = assertThrows(WebApplicationException.class, () -> {
			APIHelpers.bulkResourceClient(Patient.class, client, entryFunction, bundle);
		});

		assertEquals("failed validation", exception.getMessage());
	}
}
