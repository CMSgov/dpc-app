package gov.cms.dpc.bluebutton.client;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.param.DateRangeParam;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CapabilityStatement;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * To be clear, {@link MockBlueButtonClient} is itself only used for testing and probably doesn't need its own set of
 * tests.  SonarQube is flagging it for code coverage, though, so I'm adding some anyway.
 */
class MockBlueButtonClientUnitTest {
	private final MockBlueButtonClient client = new MockBlueButtonClient(FhirContext.forDstu3());

	private final String mbi = MockBlueButtonClient.TEST_PATIENT_MBIS.get(0);
	private final String id = MockBlueButtonClient.MBI_BENE_ID_MAP.get(mbi);

	@Test
	void testRequestPatientFromServerByMbi() {
		Bundle resultBundle = client.requestPatientFromServerByMbi(mbi, Map.of());
		assertEquals(1, resultBundle.getEntry().size());

		Patient pat = (Patient) resultBundle.getEntryFirstRep().getResource();
		assertEquals(pat.getIdPart(), id);
	}

	@Test
	void testRequestPatientFromServer() {
		DateRangeParam dateRangeParam = new DateRangeParam()
			.setLowerBound("2000-12-31")
			.setUpperBound("2100-01-01");
		Bundle resultBundle = client.requestPatientFromServer(id, dateRangeParam, Map.of());
		assertEquals(1, resultBundle.getEntry().size());

		Patient pat = (Patient) resultBundle.getEntryFirstRep().getResource();
		assertEquals(pat.getIdPart(), id);
	}
	@Test
	void testRequestPatientFromServerOutOfRange() {
		DateRangeParam dateRangeParam = new DateRangeParam()
			.setLowerBound("2099-01-01")
			.setUpperBound("2100-01-01");
		Bundle resultBundle = client.requestPatientFromServer(id, dateRangeParam, Map.of());
		assertEquals(0, resultBundle.getTotal());
	}

	@Test
	void testRequestEobFromServer() {
		DateRangeParam dateRangeParam = new DateRangeParam()
			.setLowerBound("2000-12-31")
			.setUpperBound("2100-01-01");
		Bundle resultBundle = client.requestEOBFromServer(id, dateRangeParam, Map.of());
		assertEquals("c20130ee-5bf9-4c5a-b71b-70e814b67fc0", resultBundle.getIdPart());
	}
	@Test
	void testRequestEobFromServerOutOfRange() {
		DateRangeParam dateRangeParam = new DateRangeParam()
			.setLowerBound("2099-01-01")
			.setUpperBound("2100-01-01");
		Bundle resultBundle = client.requestEOBFromServer(id, dateRangeParam, Map.of());
		assertEquals(0, resultBundle.getTotal());
	}

	@Test
	void testRequestCoverageFromServer() {
		DateRangeParam dateRangeParam = new DateRangeParam()
			.setLowerBound("2000-12-31")
			.setUpperBound("2100-01-01");
		Bundle resultBundle = client.requestCoverageFromServer(id, dateRangeParam, Map.of());
		assertEquals("5f25ebcf-a442-4811-b623-5d73d51e11e2", resultBundle.getIdPart());
	}
	@Test
	void testRequestCoverageFromServerOutOfRange() {
		DateRangeParam dateRangeParam = new DateRangeParam()
			.setLowerBound("2099-01-01")
			.setUpperBound("2100-01-01");
		Bundle resultBundle = client.requestCoverageFromServer(id, dateRangeParam, Map.of());
		assertEquals(0, resultBundle.getTotal());
	}

	@Test
	void testRequestNextBundle() {
		DateRangeParam dateRangeParam = new DateRangeParam()
			.setLowerBound("2000-12-31")
			.setUpperBound("2100-01-01");
		Bundle firstBundle = client.requestEOBFromServer(id, dateRangeParam, Map.of());

		Bundle nextBundle = client.requestNextBundleFromServer(firstBundle, Map.of());
		assertEquals("529877ac-c606-4eb2-a2e7-f64e079414a0", nextBundle.getIdPart());
	}

	@Test
	void testRequestCapabilityStatement() {
		CapabilityStatement capabilityStatement = client.requestCapabilityStatement();
		assertNotNull(capabilityStatement);
	}
}
