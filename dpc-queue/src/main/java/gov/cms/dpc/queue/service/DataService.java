package gov.cms.dpc.queue.service;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.dpc.common.annotations.ExportPath;
import gov.cms.dpc.common.annotations.JobTimeout;
import gov.cms.dpc.queue.IJobQueue;
import gov.cms.dpc.queue.JobStatus;
import gov.cms.dpc.queue.exceptions.DataRetrievalException;
import gov.cms.dpc.queue.exceptions.DataRetrievalRetryException;
import gov.cms.dpc.queue.models.JobQueueBatch;
import gov.cms.dpc.queue.models.JobQueueBatchFile;
import org.hl7.fhir.dstu3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class DataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataService.class);

    private IJobQueue queue;
    private String exportPath;
    private FhirContext fhirContext;
    private int jobTimeoutInSeconds;

    @Inject
    public DataService(IJobQueue queue, FhirContext fhirContext, @ExportPath String exportPath, @JobTimeout  int jobTimeoutInSeconds) {
        this.queue = queue;
        this.fhirContext = fhirContext;
        this.exportPath = exportPath;
        this.jobTimeoutInSeconds = jobTimeoutInSeconds;
    }

    /**
     * Retrieves data from BFD
     * @param organizationID UUID of organization
     * @param providerID UUID of provider
     * @param patientIDs List of patient String UUIDs
     * @param resourceTypes List of ResourceType data to retrieve
     * @return Resource
     */
    public Resource retrieveData(UUID organizationID,
                                 UUID providerID,
                                 List<String> patientIDs,
                                 ResourceType... resourceTypes) {
        UUID jobID = this.queue.createJob(organizationID, providerID.toString(), patientIDs, List.of(resourceTypes), null, OffsetDateTime.now(ZoneOffset.UTC));
        Optional<List<JobQueueBatch>> optionalBatches = waitForJobToComplete(jobID, organizationID, this.queue);

        if (optionalBatches.isPresent()) {
            List<JobQueueBatch> batches = optionalBatches.get();
            List<JobQueueBatchFile> files = batches.stream().map(JobQueueBatch::getJobQueueBatchFiles).flatMap(List::stream).collect(Collectors.toList());
            if (files.size() == 1 && files.get(0).getResourceType() == ResourceType.OperationOutcome) {
                return assembleOperationOutcome(batches);
            } else {
                return assembleBundleFromBatches(batches, List.of(resourceTypes));
            }
        }

        LOGGER.error("No data returned from queue for job, jobID: {}; jobTimeout: {}", jobID, jobTimeoutInSeconds);
        throw new DataRetrievalException("Failed to retrieve data");
    }

    private Optional<List<JobQueueBatch>> waitForJobToComplete(UUID jobID, UUID organizationID, IJobQueue queue) {
        CompletableFuture<Optional<List<JobQueueBatch>>> dataFuture = new CompletableFuture<>();
        final ScheduledExecutorService poller = Executors.newSingleThreadScheduledExecutor();
        final ScheduledFuture<?> task = poller.scheduleAtFixedRate(() -> {
            try {
                List<JobQueueBatch> batches = getJobBatch(jobID, organizationID, queue);
                dataFuture.complete(Optional.of(batches));
            } catch (DataRetrievalRetryException e) {
                //retrying
            }
        }, 0, 250, TimeUnit.MILLISECONDS);

        // this timeout value should probably be adjusted according to the number of types being requested
        dataFuture.completeOnTimeout(Optional.empty(), jobTimeoutInSeconds, TimeUnit.SECONDS);

        try {
            return dataFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            return Optional.empty();
        } finally {
            task.cancel(true);
            poller.shutdown();
        }
    }

    private List<JobQueueBatch> getJobBatch(UUID jobID, UUID organizationId, IJobQueue queue) throws DataRetrievalRetryException {
        final List<JobQueueBatch> batches = queue.getJobBatches(jobID);
        if (batches.isEmpty()) {
            throw new DataRetrievalRetryException();
        }

        Set<JobStatus> jobStatusSet = batches
                .stream()
                .filter(b -> b.getOrgID().equals(organizationId))
                .filter(JobQueueBatch::isValid)
                .map(JobQueueBatch::getStatus).collect(Collectors.toSet());

        if (jobStatusSet.size() == 1 && jobStatusSet.contains(JobStatus.COMPLETED)) {
            return batches;
        } else if (jobStatusSet.contains(JobStatus.FAILED)) {
            LOGGER.error("Job failed; jobID: {}, orgID: {}", jobID, organizationId);
            throw new DataRetrievalException("Failed to retrieve batches");
        } else {
            throw new DataRetrievalRetryException();
        }
    }

    private Bundle assembleBundleFromBatches(List<JobQueueBatch> batches, List<ResourceType> resourceTypes) {
        if (resourceTypes == null || resourceTypes.isEmpty()) {
            throw new DataRetrievalException("Need to pass in resource types");
        }

        final Bundle bundle = new Bundle().setType(Bundle.BundleType.SEARCHSET);

        batches.stream()
                .map(JobQueueBatch::getJobQueueBatchFiles)
                .flatMap(List::stream)
                .filter(bf -> resourceTypes.contains(bf.getResourceType()))
                .forEach(batchFile -> {
                    Path path = Paths.get(String.format("%s/%s.ndjson", exportPath, batchFile.getFileName()));
                    Class<? extends Resource> typeClass = getClassForResourceType(batchFile.getResourceType());
                    addResourceEntries(typeClass, path, bundle);
                });


        // set a bundle id here? anything else?
        bundle.setId(UUID.randomUUID().toString());
        return bundle.setTotal(bundle.getEntry().size());
    }

    private Class<? extends Resource> getClassForResourceType(ResourceType resourceType) {
        switch(resourceType) {
            case Coverage:
                return Coverage.class;
            case ExplanationOfBenefit:
                return ExplanationOfBenefit.class;
            case Patient:
                return Patient.class;
            default:
                throw new DataRetrievalException("Unexpected resource type: " + resourceType);
        }
    }

    private void addResourceEntries(Class<? extends Resource> clazz, Path path, Bundle bundle) {
        try (BufferedReader br = Files.newBufferedReader(path)) {
            br.lines().forEach(line -> {
                Resource r = fhirContext.newJsonParser().parseResource(clazz, line);
                bundle.addEntry().setResource(r);
            });
        } catch (IOException e) {
            LOGGER.error("Unable to read resource", e);
            throw new DataRetrievalException(String.format("Unable to read resource because %s", e.getMessage()));
        }
    }

    private OperationOutcome assembleOperationOutcome(List<JobQueueBatch> batches) {
        // There is only ever 1 OperationOutcome file
        final Optional<JobQueueBatchFile> batchFile = batches.stream()
                .map(b -> b.getJobQueueFileLatest(ResourceType.OperationOutcome))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();

        if (batchFile.isPresent()) {
            OperationOutcome outcome = new OperationOutcome();
            Path path = Paths.get(String.format("%s/%s.ndjson", exportPath, batchFile.get().getFileName()));
            try (BufferedReader br = Files.newBufferedReader(path)) {
                br.lines()
                        .map(line -> fhirContext.newJsonParser().parseResource(OperationOutcome.class, line))
                        .map(OperationOutcome::getIssue)
                        .flatMap(List::stream)
                        .forEach(outcome::addIssue);
            } catch (IOException e) {
                LOGGER.error("Unable to read OperationOutcome", e);
                throw new DataRetrievalException(String.format("Unable to read OperationOutcome because %s", e.getMessage()));
            }

            return outcome;
        }

        LOGGER.error("No batch files found");
        throw new DataRetrievalException("Failed to retrieve operationOutcome");
    }
}