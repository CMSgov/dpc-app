package gov.cms.dpc.bluebutton.client;

import ca.uhn.fhir.rest.api.SearchStyleEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.*;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.dpc.bluebutton.config.BBClientConfiguration;
import gov.cms.dpc.common.Constants;
import gov.cms.dpc.common.utils.MetricMaker;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import jakarta.inject.Named;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;


public class BlueButtonClientImpl implements BlueButtonClient {

    private static final String REQUEST_PATIENT_METRIC = "requestPatient";
    private static final String REQUEST_EOB_METRIC = "requestEOB";
    private static final String REQUEST_COVERAGE_METRIC = "requestCoverage";
    private static final String REQUEST_NEXT_METRIC = "requestNextBundle";
    private static final String REQUEST_CAPABILITIES_METRIC = "requestCapabilities";
    private static final List<String> REQUEST_METRICS = List.of(REQUEST_PATIENT_METRIC, REQUEST_EOB_METRIC, REQUEST_COVERAGE_METRIC, REQUEST_NEXT_METRIC, REQUEST_CAPABILITIES_METRIC);

    private static final Logger logger = LoggerFactory.getLogger(BlueButtonClientImpl.class);

    private final IGenericClient client;
    private final BBClientConfiguration config;
    private final Map<String, Timer> timers;
    private final Map<String, Meter> exceptionMeters;
    private static final String HASH_ALGORITHM = "PBKDF2WithHmacSHA256";

    private static String formBeneficiaryID(String fromPatientID) {
        return "Patient/" + fromPatientID;
    }

    public BlueButtonClientImpl(@Named("bbclient") IGenericClient client, BBClientConfiguration config, MetricRegistry metricRegistry) {
        this.client = client;
        this.config = config;
        final var metricMaker = new MetricMaker(metricRegistry, BlueButtonClientImpl.class);
        this.exceptionMeters = metricMaker.registerMeters(REQUEST_METRICS);
        this.timers = metricMaker.registerTimers(REQUEST_METRICS);
    }

    /**
     * Queries Blue Button server for patient data
     *
     * @param patientId The {@link Patient} resource's id
     * @param headers
     * @return {@link Patient} A FHIR Patient resource
     * @throws ResourceNotFoundException when no such patient with the provided ID exists
     */
    @Override
    public Bundle requestPatientFromServer(String patientId, DateRangeParam lastUpdated, Map<String, String> headers) throws ResourceNotFoundException {
        logger.debug("Attempting to fetch patient ID {} from baseURL: {}", patientId, client.getServerBase());
        ICriterion<ReferenceClientParam> criterion = new ReferenceClientParam(Patient.SP_RES_ID).hasId(patientId);
        return instrumentCall(REQUEST_PATIENT_METRIC, () ->
                fetchBundle(Patient.class, Collections.singletonList(criterion), patientId, lastUpdated, headers));
    }

    /**
     * Queries Blue Button server for patient data by MBI
     *
     * @param mbi The MBI
     * @param headers
     * @return {@link Bundle} A FHIR Bundle of Patient resources
     */
    @Override
    public Bundle requestPatientFromServerByMbi(String mbi, Map<String, String> headers) throws ResourceNotFoundException {
        return instrumentCall(REQUEST_PATIENT_METRIC, () -> {
            IQuery<IBaseBundle> query = client
                .search()
                .forResource(Patient.class)
                .where(Patient.IDENTIFIER.exactly().systemAndIdentifier(DPCIdentifierSystem.MBI.getSystem(), mbi))
                .usingStyle(SearchStyleEnum.POST);
            addBFDHeaders(query, headers);
            return query
                .returnBundle(Bundle.class)
                .execute();
        });
    }

    /**
     * Queries Blue Button server for Explanations of Benefit associated with a given patient
     * <p>
     * There are two edge cases to consider when pulling EoB data given a patientID:
     *  1. No patient with the given ID exists: if this is the case, BlueButton should return a Bundle with no
     *  entry, i.e. ret.hasEntry() will evaluate to false. For this case, the method will throw a
     *  {@link ResourceNotFoundException}
     *
     *  2. A patient with the given ID exists, but has no associated EoB records: if this is the case, BlueButton should
     *  return a Bundle with an entry of size 0, i.e. ret.getEntry().size() == 0. For this case, the method simply
     *  returns the Bundle it received from BlueButton to the caller, and the caller is responsible for handling Bundles
     *  that contain no EoBs.
     *
     * @param patientId The {@link Patient} resource's ID
     * @param headers
     * @return {@link Bundle} Containing a number (possibly 0) of {@link ExplanationOfBenefit} objects
     * @throws ResourceNotFoundException when the requested patient does not exist
     */
    @Override
    public Bundle requestEOBFromServer(String patientId, DateRangeParam lastUpdated, Map<String, String> headers) {
        logger.debug("Attempting to fetch EOBs for patient ID {} from baseURL: {}", patientId, client.getServerBase());

        List<ICriterion<? extends IParam>> criteria = new ArrayList<>();
        criteria.add(ExplanationOfBenefit.PATIENT.hasId(patientId));
        criteria.add(new TokenClientParam("excludeSAMHSA").exactly().code("true"));

        return instrumentCall(REQUEST_EOB_METRIC, () ->
                fetchBundle(ExplanationOfBenefit.class,
                        criteria,
                        patientId,
                        lastUpdated,
                        headers));
    }

    /**
     * Queries Blue Button server for Coverage associated with a given patient
     * <p>
     * Like for the EOB resource, there are two edge cases to consider when pulling coverage data given a patientID:
     *  1. No patient with the given ID exists: if this is the case, BlueButton should return a Bundle with no
     *  entry, i.e. ret.hasEntry() will evaluate to false. For this case, the method will throw a
     *  {@link ResourceNotFoundException}
     *
     *  2. A patient with the given ID exists, but has no associated Coverage records: if this is the case, BlueButton should
     *  return a Bundle with an entry of size 0, i.e. ret.getEntry().size() == 0. For this case, the method simply
     *  returns the Bundle it received from BlueButton to the caller, and the caller is responsible for handling Bundles
     *  that contain no coverage records.
     *
     * @param patientId The requested {@link Patient} resource's ID
     * @param headers
     * @return {@link Bundle} Containing a number (possibly 0) of {@link ExplanationOfBenefit} objects
     * @throws ResourceNotFoundException when the requested patient does not exist
     */
    @Override
    public Bundle requestCoverageFromServer(String patientId, DateRangeParam lastUpdated, Map<String, String> headers) throws ResourceNotFoundException {
        logger.debug("Attempting to fetch Coverage for patient ID {} from baseURL: {}", patientId, client.getServerBase());

        List<ICriterion<? extends IParam>> criteria = new ArrayList<>();
        criteria.add(Coverage.BENEFICIARY.hasId(formBeneficiaryID(patientId)));

        return instrumentCall(REQUEST_COVERAGE_METRIC, () ->
                fetchBundle(Coverage.class, criteria, patientId, lastUpdated, headers));
    }

    @Override
    public Bundle requestNextBundleFromServer(Bundle bundle, Map<String, String> headers) throws ResourceNotFoundException {
        return instrumentCall(REQUEST_NEXT_METRIC, () -> {
            var nextURL = bundle.getLink(Bundle.LINK_NEXT).getUrl();
            logger.debug("Attempting to fetch next bundle from url: {}", nextURL);
            return client
                    .loadPage()
                    .next(bundle)
                    .execute();
        });
    }

    @Override
    public CapabilityStatement requestCapabilityStatement() throws ResourceNotFoundException {
        return instrumentCall(REQUEST_CAPABILITIES_METRIC, () -> client
                        .capabilities()
                        .ofType(CapabilityStatement.class)
                        .execute());
    }

    /**
     * Read a FHIR Bundle from BlueButton. Limits the returned size by resourcesPerRequest.
     *
     * @param resourceClass - FHIR Resource class
     * @param criteria - For the resource class the correct criteria that match the patientID
     * @param patientID - id of patient
     * @param lastUpdated - the lastUpdated date to search for
     * @return FHIR Bundle resource
     */
    private <T extends IBaseResource> Bundle fetchBundle(Class<T> resourceClass,
                                                         List<ICriterion<? extends IParam>> criteria,
                                                         String patientID,
                                                         DateRangeParam lastUpdated,
                                                         Map<String, String> headers) {
        IQuery<IBaseBundle> query = client.search()
                .forResource(resourceClass)
                .where(criteria.get(0));

        for (ICriterion<? extends IParam> criterion : criteria.subList(1, criteria.size())) {
            query = query.and(criterion);
        }

        IQuery<Bundle> iQuery = query
                .count(config.getResourcesCount())
                .lastUpdated(lastUpdated)
                .returnBundle(Bundle.class);
        addBFDHeaders(query, headers);

        final Bundle bundle = iQuery.execute();

        // Case where patientID does not exist at all
        if(!bundle.hasEntry() && lastUpdated == null) {
            throw new ResourceNotFoundException("No patient found with ID: " + patientID);
        }
        return bundle;
    }

    /**
     * Instrument a call to Blue Button.
     *
     * @param metricName - The name of the method
     * @param supplier - the call as lambda to instrumented
     * @param <T> - the type returned by the call
     * @return the value returned by the supplier (i.e. call)
     */
    private <T> T instrumentCall(String metricName, Supplier<T> supplier) {
        final var timerContext = timers.get(metricName).time();
        try {
            return supplier.get();
        } catch(Exception ex) {
            final var exceptionMeter = exceptionMeters.get(metricName);
            exceptionMeter.mark();
            throw ex;
        } finally {
            timerContext.stop();
        }
    }

    private void addBFDHeaders(IQuery<?> query, Map<String, String> headers) {
        query.withAdditionalHeader(Constants.INCLUDE_IDENTIFIERS_HEADER, "mbi");
        if (headers != null) {
            headers.entrySet().stream()
                    .filter(e -> StringUtils.isNotBlank(e.getValue()))
                    .forEach(e -> query.withAdditionalHeader(e.getKey(), e.getValue()));
        }

    }
}
