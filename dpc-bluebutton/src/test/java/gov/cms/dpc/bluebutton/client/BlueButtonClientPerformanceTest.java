package gov.cms.dpc.bluebutton.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import gov.cms.dpc.bluebutton.BlueButtonClientModule;
import gov.cms.dpc.bluebutton.config.BBClientConfiguration;
import gov.cms.dpc.testing.IntegrationTest;
import org.hl7.fhir.dstu3.model.Bundle;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;

/**
 * Class used to performance test different values of the count parameter when calling BFD.  Leave this disabled so it
 * doesn't accidentally get run as part of the ci process.
 * <p>
 * As currently configured, this takes a little over 30 minutes to run.
 */
@IntegrationTest
@Disabled("Only used for performance testing.  Shouldn't be part of CI/CD process.")
class BlueButtonClientPerformanceTest {
	// List of patients in the BFD sandbox provided by the BFD team
	private final List<String> BENE_IDS = List.of(
		"-10000012009132",
		"-10000011189652",
		"-10000010255799",
		"-10000012947298",
		"-10000012571572",
		"-10000011895633",
		"-10000011044576",
		"-10000010592624",
		"-10000010365680",
		"-10000013650594"
	);

	private final int NUM_TESTS = 3;

	@Test
	void testAllBenesAndCounts() {
		BENE_IDS.forEach(this::testBeneWithAllCounts);
	}

	private void testBeneWithAllCounts(String beneId) {
		for(int count=100; count<=1000; count=count+100) {
			testBeneAndCount(beneId, count);
		}

		// One more test with no limit
		testBeneAndCount(beneId, 10000);
	}

	private void testBeneAndCount(String beneId, int count)
	{
		// Get a BFD client configured to use count
		final Injector injector = Guice.createInjector(Stage.DEVELOPMENT, new TestModule(), new BlueButtonClientModule<>(getClientConfig(count)));
		BlueButtonClient client = injector.getInstance(BlueButtonClient.class);

		// Run each test multiple times to make sure the results are consistent
		for(int i=0; i<NUM_TESTS; i++) {
			Pair<Integer, Long> testResults = runSingleTest(beneId, client);
			int cntResources = testResults.getLeft();
			float timeSeconds = (float) testResults.getRight() / 1000;
			int cntRoundTrips = (int) Math.ceil((float) cntResources / count);

			// Print out results so you can copy/paste to a spreadsheet
			System.out.printf("%s, %d, %d, %d, %.2f%n", beneId, count, cntResources, cntRoundTrips, timeSeconds);
		}
	}

	private Pair<Integer, Long> runSingleTest(String beneId, BlueButtonClient client) {
		// Get first bundle, then loop through the rest
		Instant start = Instant.now();
		LinkedList<Bundle> bundleList = new LinkedList<>(
			List.of(client.requestEOBFromServer(beneId, null, null))
		);
		while (bundleList.getLast().getLink(Bundle.LINK_NEXT) != null) {
			bundleList.add(client.requestNextBundleFromServer(bundleList.getLast(), null));
		}
		Instant end = Instant.now();

		// BFD populates total, so may as well use it
		int totalResources = bundleList.get(0).getTotal();

		return Pair.of(totalResources, Duration.between(start, end).toMillis());
	}

	private BBClientConfiguration getClientConfig(int count) {
		Config baseConfig = ConfigFactory.load("performance_test.application.conf");
		Config overrideConfig = ConfigFactory.parseString(
			"bbclient.resourcesCount = " + count
		);
		final String options = overrideConfig.withFallback(baseConfig).getConfig("bbclient").root().render(ConfigRenderOptions.concise());

		try {
			return new ObjectMapper().readValue(options, BBClientConfiguration.class);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
}
