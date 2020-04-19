package gov.cms.dpc.aggregation.engine;

import ca.uhn.fhir.context.FhirContext;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import gov.cms.dpc.bluebutton.client.BlueButtonClient;
import gov.cms.dpc.common.utils.MetricMaker;
import gov.cms.dpc.queue.IJobQueue;
import gov.cms.dpc.queue.models.JobQueueBatch;
import gov.cms.dpc.queue.models.JobQueueBatchFile;
import io.reactivex.Flowable;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.reactivestreams.Publisher;

import javax.inject.Inject;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class JobBatchProcessor {

    private final BlueButtonClient bbclient;
    private final OperationsConfig operationsConfig;
    private final FhirContext fhirContext;
    private final Meter resourceMeter;
    private final Meter operationalOutcomeMeter;

    @Inject
    public JobBatchProcessor(BlueButtonClient bbclient, FhirContext fhirContext, MetricRegistry metricRegistry, OperationsConfig operationsConfig) {
        this.bbclient = bbclient;
        this.fhirContext = fhirContext;
        this.operationsConfig = operationsConfig;

        // Metrics
        final var metricFactory = new MetricMaker(metricRegistry, JobBatchProcessor.class);
        resourceMeter = metricFactory.registerMeter("resourceFetched");
        operationalOutcomeMeter = metricFactory.registerMeter("operationalOutcomes");
    }

    /**
     * Processes a partial of a job batch. Marks the partial as completed upon processing
     *
     * @param job       - the job to process
     * @param patientID - The current patient id processing
     */
    public List<JobQueueBatchFile> processJobBatchPartial(UUID aggregatorID, IJobQueue queue, JobQueueBatch job, String patientID) {
        final var results = Flowable.fromIterable(job.getResourceTypes())
                .map(resourceType -> fetchResource(job, patientID, resourceType))
                .flatMap(result -> writeResource(job, result.getRight(), result.getLeft().flatMap(Flowable::fromIterable)))
                .toList()
                .blockingGet(); // Wait on the main thread until completion
        queue.completePartialBatch(job, aggregatorID);
        return results;
    }

    /**
     * Fetch and write a specific resource type
     *
     * @param job          context
     * @param resourceType to process
     */
    public Pair<Flowable<List<Resource>>, ResourceType> fetchResource(JobQueueBatch job, String patientID, ResourceType resourceType) {
        // Make this flow hot (ie. only called once) when multiple subscribers attach
        final var fetcher = new ResourceFetcher(bbclient, job.getJobID(), job.getBatchID(), resourceType, operationsConfig);
        return Pair.of(fetcher.fetchResources(patientID), resourceType);
    }

    private Flowable<JobQueueBatchFile> writeResource(JobQueueBatch job, ResourceType resourceType, Flowable<Resource> flow) {
        var connectableMixedFlow = flow.publish().autoConnect(2);
        // Batch the non-error resources into files
        final var resourceCount = new AtomicInteger();
        final var sequenceCount = new AtomicInteger();
        job.getJobQueueFileLatest(resourceType).ifPresent(file -> {
            resourceCount.set(file.getCount());
            sequenceCount.set(file.getSequence());
        });
        final var writer = new ResourceWriter(fhirContext, job, resourceType, operationsConfig);
        final Flowable<JobQueueBatchFile> resourceFlow = connectableMixedFlow.compose((upstream) -> bufferAndWrite(upstream, writer, resourceCount, sequenceCount));

        // Batch the error resources into files
        final var errorResourceCount = new AtomicInteger();
        final var errorSequenceCount = new AtomicInteger();
        job.getJobQueueFileLatest(ResourceType.OperationOutcome).ifPresent(file -> {
            errorResourceCount.set(file.getCount());
            errorSequenceCount.set(file.getSequence());
        });
        final var errorWriter = new ResourceWriter(fhirContext, job, ResourceType.OperationOutcome, operationsConfig);
        final Flowable<JobQueueBatchFile> outcomeFlow = connectableMixedFlow.compose((upstream) -> bufferAndWrite(upstream, errorWriter, errorResourceCount, errorSequenceCount));

        // Merge the resultant flows
        return resourceFlow.mergeWith(outcomeFlow);
    }

    /**
     * This part of the flow chain buffers resources and writes them in batches to a file
     *
     * @param writer        - the writer to use
     * @param resourceCount - the number of resources in the current file
     * @param sequenceCount - the sequence counter
     * @return a transformed flow
     */
    private Publisher<JobQueueBatchFile> bufferAndWrite(Flowable<Resource> upstream, ResourceWriter writer, AtomicInteger resourceCount, AtomicInteger sequenceCount) {
        final Flowable<Resource> filteredUpstream = upstream.filter(r -> r.getResourceType() == writer.getResourceType());
        final var connectableMixedFlow = filteredUpstream.publish().autoConnect(2);

        var resourcesInCurrentFileCount = resourceCount.getAndSet(0);
        var resourcesPerFile = operationsConfig.getResourcesPerFileCount();
        var firstResourceBatchCount = resourcesInCurrentFileCount < resourcesPerFile ? resourcesPerFile - resourcesInCurrentFileCount : resourcesPerFile;

        if (resourcesInCurrentFileCount == resourcesPerFile) {
            // Start a new file since the file has been filled up
            sequenceCount.incrementAndGet();
        }
        Meter meter = getMeter(writer.getResourceType());
        // Handle the scenario where a previous file was already written by breaking up the flow into the first batch and the buffered batch
        final Flowable<JobQueueBatchFile> partialBatch = connectableMixedFlow
                .compose(stream -> writeResources(stream.take(firstResourceBatchCount), writer, sequenceCount, meter));
        final Flowable<JobQueueBatchFile> bufferedBatch = connectableMixedFlow
                .compose(stream -> writeResources(stream.skip(firstResourceBatchCount), writer, sequenceCount, meter));

        return partialBatch.mergeWith(bufferedBatch);
    }

    private Flowable<JobQueueBatchFile> writeResources(Flowable<Resource> upstream, ResourceWriter writer, AtomicInteger sequenceCount, Meter meter) {
        return upstream
                .buffer(operationsConfig.getResourcesPerFileCount())
                .doOnNext(outcomes -> meter.mark(outcomes.size()))
                .map(batch -> writer.writeBatch(sequenceCount, batch));
    }

    private Meter getMeter(ResourceType resourceType) {
        return ResourceType.OperationOutcome == resourceType ? operationalOutcomeMeter : resourceMeter;
    }
}
