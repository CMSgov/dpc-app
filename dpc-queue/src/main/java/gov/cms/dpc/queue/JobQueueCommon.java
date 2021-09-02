package gov.cms.dpc.queue;

import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.queue.exceptions.JobQueueFailure;
import gov.cms.dpc.queue.models.JobQueueBatch;
import io.reactivex.Observable;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public abstract class JobQueueCommon implements IJobQueue {

    // Object variables
    private final int batchSize;

    public abstract void submitJobBatches(List<JobQueueBatch> jobBatches);

    protected JobQueueCommon(int batchSize) {
        this.batchSize = batchSize;
    }

    @Override
    public UUID createJob(UUID orgID, String orgNPI, String providerNPI, List<String> patients, List<DPCResourceType> resourceTypes,
                          OffsetDateTime since, OffsetDateTime transactionTime, String requestingIP, String requestUrl, boolean isBulk, boolean isSmoke) {
        final UUID jobID = UUID.randomUUID();

        List<JobQueueBatch> jobBatches;
        if (patients.isEmpty()) {
            jobBatches = createEmptyBatch(jobID, orgID, orgNPI, providerNPI, resourceTypes, since, transactionTime);
        } else if (since != null && !transactionTime.isAfter(since)) {
            // If the since request is after the BFD transactionTime, then result will always be an empty result set
            jobBatches = createEmptyBatch(jobID, orgID, orgNPI, providerNPI, resourceTypes, since, transactionTime);
        } else {
            jobBatches = Observable.fromIterable(patients)
                    .buffer(batchSize)
                    .map(patientBatch -> this.createJobBatch(jobID, orgID, orgNPI, providerNPI, patientBatch, resourceTypes,
                            since, transactionTime, requestingIP, requestUrl, isBulk))
                    .toList()
                    .blockingGet();
        }

        // Set the priority of a job batch
        // Single patients will have first priority to support patient everything
        final int priority = determinePriority(patients.size(), isSmoke);
        jobBatches.forEach(batch -> batch.setPriority(priority));

        this.submitJobBatches(jobBatches);
        return jobBatches.stream().map(JobQueueBatch::getJobID).findFirst().orElseThrow(() -> new JobQueueFailure("Unable to create job. No batches to submit."));
    }

    private int determinePriority(int patientCount, boolean isSmoke){
        if (isSmoke){
            return 1500;
        }else if (patientCount == 1){
            return 1000;
        }else{
            return 5000;
        }
    }

    protected JobQueueBatch createJobBatch(UUID jobID,
                                           UUID orgID,
                                           String orgNPI,
                                           String providerNPI,
                                           List<String> patients,
                                           List<DPCResourceType> resourceTypes,
                                           OffsetDateTime since,
                                           OffsetDateTime transactionTime,
                                           String requestingIP,
                                           String requestUrl,
                                           boolean isBulk) {
        return new JobQueueBatch(jobID, orgID, orgNPI, providerNPI, patients, resourceTypes, since, transactionTime, requestingIP, requestUrl, isBulk);
    }

    protected List<JobQueueBatch> createEmptyBatch(UUID jobID,
                                                   UUID orgID,
                                                   String orgNPI,
                                                   String providerNPI,
                                                   List<DPCResourceType> resourceTypes,
                                                   OffsetDateTime since,
                                                   OffsetDateTime transactionTime) {
        return Collections.singletonList(
                createJobBatch(jobID, orgID, orgNPI, providerNPI, Collections.emptyList(), resourceTypes, since, transactionTime, null, null, true)
        );
    }
}
