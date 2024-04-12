package gov.cms.dpc.aggregation.engine;

import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import gov.cms.dpc.bluebutton.client.BlueButtonClient;
import gov.cms.dpc.common.MDCConstants;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.queue.exceptions.JobQueueFailure;
import io.reactivex.Flowable;
import org.hl7.fhir.dstu3.model.*;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static gov.cms.dpc.fhir.FHIRExtractors.getPatientMBI;

/**
 * A resource fetcher will fetch resources of particular type from passed {@link BlueButtonClient}
 */
class ResourceFetcher {
    private static final Logger logger = LoggerFactory.getLogger(ResourceFetcher.class);
    private final BlueButtonClient blueButtonClient;
    private final UUID jobID;
    private final UUID batchID;
    private final DPCResourceType resourceType;
    private final OffsetDateTime since;
    private final OffsetDateTime transactionTime;

    /**
     * Create a context for fetching FHIR resources
     * @param blueButtonClient - client to BlueButton to use
     * @param jobID - the jobID for logging and reporting
     * @param batchID - the batchID for logging and reporting
     * @param resourceType - the resource type to fetch
     * @param since - the since parameter for the job
     * @param transactionTime - the start time of this job
     */
    ResourceFetcher(BlueButtonClient blueButtonClient,
                    UUID jobID,
                    UUID batchID,
                    DPCResourceType resourceType,
                    OffsetDateTime since,
                    OffsetDateTime transactionTime) {
        this.blueButtonClient = blueButtonClient;
        this.jobID = jobID;
        this.batchID = batchID;
        this.resourceType = resourceType;
        this.since = since;
        this.transactionTime = transactionTime;
    }

    /**
     * Fetch all the resources for a specific patient. If errors are encountered from BlueButton,
     * a OperationOutcome resource is used.
     *
     * @param patient {@link Patient} we're fetching resources for
     * @param headers headers
     * @return a flow with all the resources for specific patient
     */
    Flowable<List<Resource>> fetchResources(Patient patient, Map<String, String> headers) {
        return Flowable.fromCallable(() -> {
            String fetchId = UUID.randomUUID().toString();
            logger.debug("Fetching first {} from BlueButton for {}", resourceType.toString(), fetchId);
            final Bundle firstFetched = fetchFirst(patient, headers);
            return fetchAllBundles(firstFetched, fetchId, headers);
        })
                .onErrorResumeNext((Throwable error) -> handleError(patient, error));
    }

    /**
     * Given a bundle, return a list of resources in the passed in bundle and all
     * the resources from the next bundles.
     *
     * @param firstBundle of resources. Included in the result list
     * @return a list of all the resources in the first bundle and all next bundles
     */
    private List<Resource> fetchAllBundles(Bundle firstBundle, String fetchId,  Map<String, String> headers) {
        final var resources = new ArrayList<Resource>();
        checkBundleTransactionTime(firstBundle);
        addResources(resources, firstBundle);

        // Loop until no more next bundles
        var bundle = firstBundle;
        while (bundle.getLink(Bundle.LINK_NEXT) != null) {
            logger.debug("Fetching next bundle {} from BlueButton for {}", resourceType.toString(), fetchId);
            bundle = blueButtonClient.requestNextBundleFromServer(bundle, headers);
            checkBundleTransactionTime(bundle);
            addResources(resources, bundle);
        }

        logger.debug("Done fetching bundles {} for {}", resourceType.toString(), fetchId);
        return resources;
    }

    /**
     * Turn an error into a flow.
     * @param patient The {@link Patient} we're loading resources for
     * @param error the error
     * @return a Flowable of list of resources
     */
    private Publisher<List<Resource>> handleError(Patient patient, Throwable error) {
        if (error instanceof JobQueueFailure) {
            // JobQueueFailure is an internal error. Just pass it along as an error.
            return Flowable.error(error);
        }

        // Other errors should be turned into OperationOutcome and just recorded.
        logger.error("Turning error into OperationOutcome.", error);
        final var operationOutcome = formOperationOutcome(getPatientMBI(patient), error);
        return Flowable.just(List.of(operationOutcome));
    }

    /**
     * Based on resourceType, fetch a resource or a bundle of resources.
     *
     * @param patient   {@link Patient} we're fetching resources for
     * @return the first bundle of resources
     */
    private Bundle fetchFirst(Patient patient, Map<String, String> headers) {
        patient.getIdentifier().stream()
                .filter(i -> i.getSystem().equals(DPCIdentifierSystem.MBI_HASH.getSystem()))
                .findFirst()
                .ifPresent(i -> MDC.put(MDCConstants.PATIENT_ID, i.getValue()));

        String patientId = patient.getIdElement().getIdPart();

        final var lastUpdated = formLastUpdatedParam();
        switch (resourceType) {
            // Why are we reloading the Patient, because on reload we're passing a lastUpdated parameter.  If the user
            // only wants resources from the last month and our patient hasn't been updated in a year, this method will
            // return an empty bundle.
            // TODO: Implement the since parameter ourselves using meta.lastUpdated and avoid pulling Patient and EoB twice.
            case Patient:
                Bundle resultBundle = new Bundle();
                Date patientLastUpdated = patient.getMeta().getLastUpdated();
                if (patientLastUpdated == null) {
                    return blueButtonClient.requestPatientFromServer(patientId, lastUpdated, headers);
                } else if (since != null && patientLastUpdated.toInstant().isBefore(since.toInstant())) {
                    return resultBundle;
                } else {
                    resultBundle.addEntry().setResource(patient);
                    return resultBundle;
                }
            case ExplanationOfBenefit:
                return blueButtonClient.requestEOBFromServer(patientId, lastUpdated, headers);
            case Coverage:
                return blueButtonClient.requestCoverageFromServer(patientId, lastUpdated, headers);
            default:
                throw new JobQueueFailure(jobID, batchID, "Unexpected resource type: " + resourceType);
        }
    }

    /**
     * Add resources in a bundle to a list
     *
     * @param resources - the list to add resources to
     * @param bundle - the bundle to extract resources from
     */
    private void addResources(ArrayList<Resource> resources, Bundle bundle) {
        bundle.getEntry().forEach((entry) -> {
            final var resource = entry.getResource();
            if (resource.getResourceType().getPath() != resourceType.getPath()) {
                throw new DataFormatException(String.format("Unexpected resource type: got %s expected: %s", resource.getResourceType().toString(), resourceType.toString()));
            }
            resources.add(resource);
        });
    }

    /**
     * Create a {@link OperationOutcome} resource from an exception with a patient
     *
     * @param ex - the exception to turn into a Operation Outcome
     * @return an operation outcome
     */
    private OperationOutcome formOperationOutcome(String patientID, Throwable ex) {
        String details;
        if (ex instanceof ResourceNotFoundException) {
            details = String.format("%s resource not found in Blue Button for id: %s", resourceType.toString(), patientID);
        } else if (ex instanceof BaseServerResponseException) {
            final var serverException = (BaseServerResponseException) ex;
            details = String.format("Blue Button error fetching %s resource. HTTP return code: %s", resourceType.toString(), serverException.getStatusCode());
        } else {
            details = String.format("Internal error: %s", ex.getMessage());
        }

        final var patientLocation = List.of(new StringType("Patient"), new StringType("id"), new StringType(patientID));
        final var outcome = new OperationOutcome();
        outcome.addIssue()
                .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                .setCode(OperationOutcome.IssueType.EXCEPTION)
                .setDetails(new CodeableConcept().setText(details))
                .setLocation(patientLocation);
        return outcome;
    }

    /**
     * Form a date range for the lastUpdated parameter for this export job
     *
     * @return a date range for this job
     */
    @SuppressWarnings("JdkObsolete") // Date class is used by HAPI FHIR DateRangeParam
    private DateRangeParam formLastUpdatedParam() {
        // Note: FHIR bulk spec says that since is exclusive and transactionTime is inclusive
        // It is also says that all resources should not have lastUpdated after the transactionTime.
        // This is true for the both the since and the non-since cases.
        // BFD will include resources that do not have a lastUpdated if there isn't a complete range.
        return since != null ?
                new DateRangeParam()
                        .setUpperBoundInclusive(Date.from(transactionTime.toInstant()))
                        .setLowerBoundExclusive(Date.from(since.toInstant())) :
                new DateRangeParam()
                        .setUpperBoundInclusive(Date.from(transactionTime.toInstant()));
    }

    /**
     * Check the transaction time of the BFD against the transaction time of the export job
     *
     * @param bundle to check
     */
    @SuppressWarnings("JdkObsolete") // Date class is used by FHIR stu3 Meta model
    private void checkBundleTransactionTime(Bundle bundle) {
        if (bundle.getMeta() == null || bundle.getMeta().getLastUpdated() == null) return;
        final var bfdTransactionTime = bundle.getMeta().getLastUpdated().toInstant().atOffset(ZoneOffset.UTC);
        if (bfdTransactionTime.isBefore(transactionTime)) {
           // See BFD's RFC0004 for a discussion on why this type error may occur.
           // Note: Retrying the job after a delay may fix this problem.
            logger.error("Failing the job for a BFD transaction time regression: BFD time {}, Job time {}",
                    bfdTransactionTime,
                    transactionTime);
            throw new JobQueueFailure("BFD's transaction time regression");
        }
    }
}
