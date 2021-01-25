package gov.cms.dpc.bluebutton.client;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.PerformanceOptionsEnum;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.apache.http.client.utils.URLEncodedUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CapabilityStatement;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class MockBlueButtonClient implements BlueButtonClient {

    private static final String SAMPLE_EOB_PATH_PREFIX = "bb-test-data/eob/";
    private static final String SAMPLE_PATIENT_PATH_PREFIX = "bb-test-data/patient/";
    private static final String SAMPLE_COVERAGE_PATH_PREFIX = "bb-test-data/coverage/";
    private static final String SAMPLE_METADATA_PATH_PREFIX = "bb-test-data/";
    private static final String SAMPLE_EMPTY_BUNDLE = "bb-test-data/empty.xml";

    public static final List<String> TEST_PATIENT_MBIS = List.of("2SW4N00AA00", "4SP0P00AA00", "3S58A00AA00", "4S58A00AA00", "5S58A00AA00", "1SQ3F00AA00");
    public static final Map<String, String> MBI_BENE_ID_MAP = Map.of(
            TEST_PATIENT_MBIS.get(0), "-20140000008325",
            TEST_PATIENT_MBIS.get(1), "-20140000009893",
            TEST_PATIENT_MBIS.get(2), "-19990000002208",
            TEST_PATIENT_MBIS.get(3), "-19990000002209",
            TEST_PATIENT_MBIS.get(4), "-19990000002210",
            TEST_PATIENT_MBIS.get(5), "-20000000001809"




            );
    public static final Map<String, String> MBI_HASH_MAP = Map.of(
            TEST_PATIENT_MBIS.get(0), "abadf57ff8dc94610ca0d479feadb1743c9cd3c77caf1eafde5719a154379fb6",
            TEST_PATIENT_MBIS.get(1), "8930cab29ba5fe4311a5f5bcfd5b7384f3722b711402aacf796d2ae6fea54242",
            TEST_PATIENT_MBIS.get(2), "e411277fd31da392eaa9a45df53b0c429e365626182f50d9f35810d77f0e2756",
            TEST_PATIENT_MBIS.get(3), "41af07535e0a66226cf2f0e6c551c0a15bd49192fc055aa5cd2e63f31f90a419",
            TEST_PATIENT_MBIS.get(4), "d35350fce12f555089f938c0323a13122622123038e8af057a4191fd450c2b90",
            TEST_PATIENT_MBIS.get(5), "a006edba97087f2911a35706e46bf1287d21d8fa515024ace44d589bdef9d819"

    );
    public static final List<String> TEST_PATIENT_WITH_BAD_IDS = List.of("-1", "-2", TEST_PATIENT_MBIS.get(0), TEST_PATIENT_MBIS.get(1), "-3");
    public static final String MULTIPLE_RESULTS_MBI = "0SW4N00AA00";
    public static final OffsetDateTime BFD_TRANSACTION_TIME = OffsetDateTime.ofInstant(Instant.now().truncatedTo(ChronoUnit.MILLIS), ZoneOffset.UTC);
    public static final OffsetDateTime TEST_LAST_UPDATED = OffsetDateTime.parse("2020-01-01T00:00:00-05:00");

    private final IParser parser;

    public MockBlueButtonClient(FhirContext fhirContext) {
        fhirContext.setPerformanceOptions(PerformanceOptionsEnum.DEFERRED_MODEL_SCANNING);
        parser = fhirContext.newXmlParser();
    }

    @Override
    public Bundle requestPatientFromServerByMbi(String mbi, Map<String, String> headers) throws ResourceNotFoundException {
        return loadBundle(SAMPLE_PATIENT_PATH_PREFIX, MBI_BENE_ID_MAP.getOrDefault(mbi,""));
    }

    @Override
    public Bundle requestPatientFromServer(String beneId, DateRangeParam lastUpdated, Map<String, String> headers) throws ResourceNotFoundException {
        return isInDateRange(lastUpdated) ?
                loadBundle(SAMPLE_PATIENT_PATH_PREFIX, beneId) :
                loadEmptyBundle();
    }


    @Override
    public Bundle requestPatientFromServerByMbiHash(String mbiHash, Map<String, String> headers) throws ResourceNotFoundException {
        String mbi = MBI_HASH_MAP.values().stream()
                .filter(h -> h.equals(mbiHash))
                .findFirst()
                .orElse("");
        return loadBundle(SAMPLE_PATIENT_PATH_PREFIX, MBI_BENE_ID_MAP.get(mbi));
    }

    @Override
    public Bundle requestEOBFromServer(String beneId, DateRangeParam lastUpdated, Map<String, String> headers) throws ResourceNotFoundException {
        return isInDateRange(lastUpdated) ?
                loadBundle(SAMPLE_EOB_PATH_PREFIX, beneId) :
                loadEmptyBundle();
    }

    @Override
    public Bundle requestCoverageFromServer(String beneId, DateRangeParam lastUpdated, Map<String, String> headers) throws ResourceNotFoundException {
        return isInDateRange(lastUpdated) ?
                loadBundle(SAMPLE_COVERAGE_PATH_PREFIX, beneId) :
                loadEmptyBundle();
    }

    @Override
    @SuppressWarnings("JdkObsolete") // Date class is used by FHIR stu3 Meta model
    public Bundle requestNextBundleFromServer(Bundle bundle, Map<String, String> headers) throws ResourceNotFoundException {
        // This is code is very specific to the bb-test-data directory and its contents
        final var nextLink = bundle.getLink(Bundle.LINK_NEXT).getUrl();
        final var nextUrl = URI.create(nextLink);
        final var params = URLEncodedUtils.parse(nextUrl.getQuery(), StandardCharsets.UTF_8);
        final var patient = params.stream().filter(pair -> pair.getName().equals("patient")).findFirst().orElseThrow().getValue();
        final var startIndex = params.stream().filter(pair -> pair.getName().equals("startIndex")).findFirst().orElseThrow().getValue();
        var path = SAMPLE_EOB_PATH_PREFIX + patient + "_" + startIndex + ".xml";

        try(InputStream sampleData = MockBlueButtonClient.class.getClassLoader().getResourceAsStream(path)) {
            final var nextBundle = parser.parseResource(Bundle.class, sampleData);
            nextBundle.getMeta().setLastUpdated(Date.from(BFD_TRANSACTION_TIME.toInstant()));
            return nextBundle;
        } catch(IOException ex) {
            throw new ResourceNotFoundException("Missing next bundle");
        }
    }

    @Override
    public CapabilityStatement requestCapabilityStatement() throws ResourceNotFoundException {
        final var path = SAMPLE_METADATA_PATH_PREFIX + "meta.xml";
        try(InputStream sampleData = loadResource(path, null)) {
            return parser.parseResource(CapabilityStatement.class, sampleData);
        } catch(IOException ex) {
            throw formNoPatientException(null);
        }
    }

    @Override
    public String hashMbi(String mbi) {
        return MBI_HASH_MAP.get(mbi);
    }

    /**
     * Read a Bundle FHIR Resource from jar's Bundle resource file.
     *
     * @param pathPrefix - Path to the XML sample data
     * @param beneId - CCW/BFD beneficiary ID of patient (https://bluebutton.cms.gov/resources/variables/bene_id)
     * @return FHIR Resource
     */
    @SuppressWarnings("JdkObsolete") // Date class is used by FHIR stu3 Meta model
    private Bundle loadBundle(String pathPrefix, String beneId) {
        try(InputStream sampleData = loadResource(pathPrefix, beneId)) {
            final var bundle = parser.parseResource(Bundle.class, sampleData);
            bundle.getMeta().setLastUpdated(Date.from(BFD_TRANSACTION_TIME.toInstant()));
            return bundle;
        } catch(IOException ex) {
            throw formNoPatientException(beneId);
        }
    }

    /**
     * Create a stream from a resource.
     *
     * @param pathPrefix - The path to the resource file
     * @param beneId - The beneficiary ID of the patient associated with the file
     * @return the stream associated with the resource
     */
    private InputStream loadResource(String pathPrefix, String beneId) throws ResourceNotFoundException {
        if (!MBI_BENE_ID_MAP.containsValue(beneId)) {
            throw formNoPatientException(beneId);
        }
        final var path = pathPrefix + beneId + ".xml";
        return MockBlueButtonClient.class.getClassLoader().getResourceAsStream(path);
    }

    /**
     * Does the passed in date range matches resources without lastUpdated
     *
     * @param range to test
     * @return true iff date range matches
     */
    @SuppressWarnings("JdkObsolete") // Date class is used by HAPI FHIR DateRangeParam
    private boolean isInDateRange(DateRangeParam range) {
        if (range == null) return true;
        final var upperBound = range.getUpperBoundAsInstant();
        final var lowerBound = range.getLowerBoundAsInstant();
        return (upperBound == null || upperBound.toInstant().isAfter(TEST_LAST_UPDATED.toInstant())) &&
            (lowerBound == null || lowerBound.toInstant().isBefore(TEST_LAST_UPDATED.toInstant()));
    }

    /**
     * Return an empty bundle.
     *
     * @return a Bundle
     */
    @SuppressWarnings("JdkObsolete") // Date class is used by FHIR stu3 Meta model
    private Bundle loadEmptyBundle() {
        try(InputStream sampleData = MockBlueButtonClient.class.getClassLoader().getResourceAsStream(SAMPLE_EMPTY_BUNDLE)) {
            final var bundle = parser.parseResource(Bundle.class, sampleData);
            bundle.getMeta().setLastUpdated(Date.from(BFD_TRANSACTION_TIME.toInstant()));
            return bundle;
        } catch(IOException ex) {
            throw formNoPatientException(null);
        }
    }

    private ResourceNotFoundException formNoPatientException(String patientID) {
        return new ResourceNotFoundException("No patient found with ID: " + patientID);
    }
}
