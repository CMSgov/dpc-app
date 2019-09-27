package gov.cms.dpc.queue;

import gov.cms.dpc.queue.exceptions.JobQueueFailure;
import gov.cms.dpc.queue.models.JobQueueBatch;
import io.reactivex.Observable;
import org.hl7.fhir.dstu3.model.ResourceType;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public abstract class JobQueueCommon implements IJobQueue {

    // Object variables
    private final int batchSize;

    protected abstract void submitJobBatches(List<JobQueueBatch> jobBatches);

    public JobQueueCommon(int batchSize) {
        this.batchSize = batchSize;
    }

    @Override
    public UUID createJob(UUID orgID, String providerID, List<String> patients, List<ResourceType> resourceTypes) {
        final UUID jobID = UUID.randomUUID();

        List<JobQueueBatch> jobBatches = Observable.fromIterable(patients)
                .buffer(batchSize)
                .map(patientBatch -> this.createJobBatch(jobID, orgID, providerID, patientBatch, resourceTypes))
                .toList()
                .blockingGet();

        // Expect a single empty job when no patients passed
        if ( jobBatches.isEmpty() && patients.isEmpty() ) {
            jobBatches.add(this.createJobBatch(jobID, orgID, providerID, Collections.emptyList(), resourceTypes));
        }

        // Set the priority of a job batch
        // Single patients will have first priority to support patient everything
        final int priority = patients.size() == 1 ? 1000 : 5000;
        jobBatches.forEach(batch -> batch.setPriority(priority));

        this.submitJobBatches(jobBatches);
        return jobBatches.stream().map(JobQueueBatch::getJobID).findFirst().orElseThrow(() -> new JobQueueFailure("Unable to create job. No batches to submit."));
    }

    protected JobQueueBatch createJobBatch(UUID jobID, UUID orgID, String providerID, List<String> patients, List<ResourceType> resourceTypes) {
        return new JobQueueBatch(jobID, orgID, providerID, patients, resourceTypes);
    }

    public int getBatchSize() {
        return batchSize;
    }
}
