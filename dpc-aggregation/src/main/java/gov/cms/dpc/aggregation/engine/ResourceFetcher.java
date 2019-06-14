package gov.cms.dpc.aggregation.engine;

import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import gov.cms.dpc.bluebutton.client.BlueButtonClient;
import gov.cms.dpc.queue.exceptions.JobQueueFailure;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.reactivex.Emitter;
import io.reactivex.Observable;
import io.reactivex.subjects.Subject;
import org.hl7.fhir.dstu3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * A resource fetcher will fetch resources of particular type from passed {@link BlueButtonClient}
 */
class ResourceFetcher {
    private static final Logger logger = LoggerFactory.getLogger(ResourceFetcher.class);
    private BlueButtonClient blueButtonClient;
    private RetryConfig retryConfig;
    private UUID jobID;
    private ResourceType resourceType;
    private Subject<Resource> errorSubject;

    /**
     * Create a context for fetching FHIR resources
     * @param blueButtonClient - client to BlueButton to use
     * @param retryConfig - retry parameters
     * @param jobID - the jobID for logging and reporting
     * @param resourceType - the resource type to fetch
     * @param errorSubject - {@link OperationOutcome}s are put here
     */
    ResourceFetcher(BlueButtonClient blueButtonClient,
                           RetryConfig retryConfig,
                           UUID jobID,
                           ResourceType resourceType,
                           Subject<Resource> errorSubject) {
        this.blueButtonClient = blueButtonClient;
        this.retryConfig = retryConfig;
        this.jobID = jobID;
        this.resourceType = resourceType;
        this.errorSubject = errorSubject;
    }

    /**
     * Fetches the given resource from the {@link BlueButtonClient} and converts it from FHIR-JSON to Resource. The
     * resource may be a type requested or it may be an operational outcome;
     * @param patientID - the patient to fetch
     * @return an observable for the resources
     */
    Observable<Resource> fetchResources(String patientID) {
        return Observable.create(emitter -> {
            try {
                // Fetch the resource in a retry loop
                logger.debug("Fetching {} from BlueButton for {}", resourceType.toString(), jobID);
                Retry retry = Retry.of("bb-resource-fetcher", this.retryConfig);
                final var fetchFirstDecorated = Retry.decorateFunction(retry, supplyBBMethod());
                final Resource firstResource = fetchFirstDecorated.apply(patientID);
                emitter.onNext(firstResource);

                // If this is a bundle, fetch the next bundles
                if (firstResource.getResourceType() == ResourceType.Bundle) {
                    fetchAllNextBundles(emitter, patientID, (Bundle)firstResource);
                }

                // All done
                emitter.onComplete();
            } catch (JobQueueFailure ex) {
                // Fatal for this job
                emitter.onError(ex);
            } catch(Exception ex){
                // Otherwise, capture the BB error.
                // Per patient errors are not fatal for the job, just turn them into operation outcomes
                logger.error("Error fetching from Blue Button for a patient", ex);
                errorSubject.onNext(formOperationOutcome(patientID, ex));
                emitter.onComplete();
            }
        });
    }

    /**
     * Returns the associated blueButtonClient method
     * @return a method to that fetches a resource
     */
    private Function<String, Resource> supplyBBMethod() {
        switch (resourceType) {
            case Patient:
                return blueButtonClient::requestPatientFromServer;
            case ExplanationOfBenefit:
                return blueButtonClient::requestEOBFromServer;
            case Coverage:
                return blueButtonClient::requestCoverageFromServer;
            default:
                throw new JobQueueFailure(jobID, "Unexpected resource type: " + resourceType.toString());
        }
    }

    /**
     *  Fetch the all the next bundles if there are any.
     *
     * @param emitter to write the bundles to
     * @param firstBundle to get the next link
     */
    private void fetchAllNextBundles(Emitter<Resource> emitter, String patientID, Bundle firstBundle) {
        for (var bundle = firstBundle; bundle.getLink(Bundle.LINK_NEXT) != null; ) {
            Retry retry = Retry.of("bb-resource-fetcher", this.retryConfig);
            final var decorated = Retry.decorateFunction(retry, blueButtonClient::requestNextBundleFromServer);
            try {
                logger.debug("Fetching next {} from BlueButton", resourceType.toString());
                bundle = decorated.apply(bundle);
                emitter.onNext(bundle);
            } catch (Exception ex) {
                errorSubject.onNext(formOperationOutcome(patientID, ex));
                logger.error("Error fetching the next bundle from Blue Button", ex);
            }
        }
    }

    /**
     * Check for bundle resources. Unpack bundle resources into their constituent resources, otherwise do nothing. Used
     * in conjunction with flatMap or concatMap operators.
     *
     * @param resource - The resource to examine and possibly unpack
     * @return A stream of resources
     */
    Observable<Resource> unpackBundles(Resource resource) {
        if (resource.getResourceType() != ResourceType.Bundle) {
            return Observable.just(resource);
        }
        final Bundle bundle = (Bundle)resource;
        final Resource[] entries = bundle.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toArray(Resource[]::new);
        return Observable.fromArray(entries);
    }

    /**
     * Create a OperationalOutcome resource from an exception with a patient
     *
     * @param ex        - the exception to turn into a Operational Outcome
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
}
